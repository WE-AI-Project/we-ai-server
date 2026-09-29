package com.weai.server.domain.ai.debate;

import com.weai.server.domain.ai.debate.agent.BackendAi;
import com.weai.server.domain.ai.debate.agent.FrontendAi;
import com.weai.server.domain.ai.debate.agent.InspectorAi;
import com.weai.server.domain.ai.debate.agent.OracleAi;
import com.weai.server.domain.ai.rag.ProjectRagRetriever;
import com.weai.server.domain.ai.rag.RagContextSanitizer;
import com.weai.server.domain.ai.rag.ThinkingLevel;
import com.weai.server.domain.ai.support.AiCallRetrier;
import com.weai.server.domain.user.domain.User;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class AiDebateService {

	// 개별 에이전트 호출(callAgent)이 이 안에 못 끝나면 ai.debate.timeout에서 자체적으로
	// 실패하므로, 그 두 배 여유를 스트림 전체 타임아웃으로 잡아 정상적인 다회전 토론은 절대
	// 중간에 끊기지 않게 한다.
	private static final long DEBATE_STREAM_TIMEOUT_MS = 10 * 60 * 1000L;

	private final OracleAi oracleAi;
	private final BackendAi backendAi;
	private final FrontendAi frontendAi;
	private final InspectorAi inspectorAi;
	private final ProjectRagRetriever projectRagRetriever;
	private final AgentMetricsService agentMetricsService;
	private final Executor debateStreamExecutor;
	private final int maxRounds;

	public AiDebateService(
		OracleAi oracleAi,
		BackendAi backendAi,
		FrontendAi frontendAi,
		InspectorAi inspectorAi,
		@Lazy ProjectRagRetriever projectRagRetriever,
		AgentMetricsService agentMetricsService,
		@Qualifier("aiDebateStreamExecutor") Executor debateStreamExecutor,
		@Value("${ai.debate.max-rounds:10}") int maxRounds
	) {
		this.oracleAi = oracleAi;
		this.backendAi = backendAi;
		this.frontendAi = frontendAi;
		this.inspectorAi = inspectorAi;
		this.projectRagRetriever = projectRagRetriever;
		this.agentMetricsService = agentMetricsService;
		this.debateStreamExecutor = debateStreamExecutor;
		this.maxRounds = Math.max(1, maxRounds);
	}

	public DebateResponse debate(User user, Long projectId, EditorContextDto context) {
		return debate(user, projectId, context, List.of(
			AiAgentType.ORACLE,
			AiAgentType.BACKEND,
			AiAgentType.FRONTEND,
			AiAgentType.INSPECTOR
		), maxRounds);
	}

	public DebateResponse debate(
		User user,
		Long projectId,
		EditorContextDto context,
		List<AiAgentType> selectedAgents,
		Integer requestedMaxRounds
	) {
		validate(user, projectId, context);
		List<AiAgentType> agents = normalizeAgents(selectedAgents);
		int roundLimit = normalizeMaxRounds(requestedMaxRounds);
		validateTerminationIsReachable(agents, requestedMaxRounds);
		String codeSnippet = DebateContentGuard.sanitizeCodeSnippet(context.currentCodeSnippet().trim());

		List<String> ragContexts = projectRagRetriever.retrieve(projectId, buildRagQuery(context, codeSnippet), ThinkingLevel.from(context.level()));
		String ragContext = formatRagContext(projectId, ragContexts);

		StringBuilder debateHistory = new StringBuilder();
		debateHistory
			.append("[Request Context]\n")
			.append("User: ").append(user.getEmail()).append("\n")
			.append("Project ID: ").append(projectId).append("\n")
			.append("Project-isolated RAG documents: ").append(ragContexts.size()).append(" chunks\n")
			.append(ragContext);

		List<DebateResponse.DebateTurn> turns = new ArrayList<>();
		boolean completed = false;
		int executedRounds = 0;

		String lastOracleOpinion = "";
		String lastBackendOpinion = "";
		String lastFrontendOpinion = "";
		String lastInspectorOpinion = "";

		for (int round = 1; round <= roundLimit; round++) {
			executedRounds = round;

			for (AiAgentType agent : agents) {
				String opinion = callAgent(agent, context, codeSnippet, projectId, ragContext, round, debateHistory);
				appendTurn(debateHistory, turns, round, agent, opinion);

				switch (agent) {
					case ORACLE -> lastOracleOpinion = opinion;
					case BACKEND -> lastBackendOpinion = opinion;
					case FRONTEND -> lastFrontendOpinion = opinion;
					case INSPECTOR -> lastInspectorOpinion = opinion;
				}

				if (agent == AiAgentType.INSPECTOR && isDebateEndSignal(opinion)) {
					completed = true;
					break;
				}
			}

			if (completed) {
				break;
			}
		}

		String markdown = buildMarkdown(
			context,
			projectId,
			ragContexts.size(),
			completed,
			executedRounds,
			roundLimit,
			agents,
			debateHistory.toString(),
			lastOracleOpinion,
			lastBackendOpinion,
			lastFrontendOpinion,
			lastInspectorOpinion
		);

		return new DebateResponse(
			projectId,
			context.fileName().trim(),
			context.cursorLine(),
			context.userQuery().trim(),
			completed,
			executedRounds,
			roundLimit,
			lastOracleOpinion,
			lastBackendOpinion,
			lastFrontendOpinion,
			lastInspectorOpinion,
			debateHistory.toString(),
			markdown,
			List.copyOf(ragContexts),
			List.copyOf(turns)
		);
	}

	// SSE 버전 토론. 백엔드→프론트엔드 응답 하나에 4개 에이전트 x N라운드를 전부 담아서 돌려주면
	// (기존 debate()) Ollama 호출이 누적되어 Cloudflare/브라우저의 게이트웨이 타임아웃을 넘기기
	// 쉽다. 같은 라운드-로빈 루프를 그대로 쓰되, 에이전트 한 명이 응답할 때마다 즉시 SSE로
	// 흘려보내서 (1) 타임아웃을 피하고 (2) 프론트에서 에이전트들이 실시간으로 대화하는 것처럼
	// 보이게 한다.
	public SseEmitter debateStream(
		User user,
		Long projectId,
		EditorContextDto context,
		List<AiAgentType> selectedAgents,
		Integer requestedMaxRounds
	) {
		validate(user, projectId, context);
		List<AiAgentType> agents = normalizeAgents(selectedAgents);
		int roundLimit = normalizeMaxRounds(requestedMaxRounds);
		validateTerminationIsReachable(agents, requestedMaxRounds);

		SseEmitter emitter = new SseEmitter(DEBATE_STREAM_TIMEOUT_MS);
		try {
			debateStreamExecutor.execute(() -> runDebateStream(emitter, user, projectId, context, agents, roundLimit));
		} catch (TaskRejectedException exception) {
			log.warn("AI debate stream rejected for projectId={}: too many concurrent debate streams.", projectId);
			emitter.completeWithError(new ApiException(ErrorCode.TOO_MANY_REQUESTS));
		}

		return emitter;
	}

	private void runDebateStream(
		SseEmitter emitter,
		User user,
		Long projectId,
		EditorContextDto context,
		List<AiAgentType> agents,
		int roundLimit
	) {
		try {
			String codeSnippet = DebateContentGuard.sanitizeCodeSnippet(context.currentCodeSnippet().trim());
			List<String> ragContexts = projectRagRetriever.retrieve(projectId, buildRagQuery(context, codeSnippet), ThinkingLevel.from(context.level()));
			String ragContext = formatRagContext(projectId, ragContexts);

			StringBuilder debateHistory = new StringBuilder()
				.append("[Request Context]\n")
				.append("User: ").append(user.getEmail()).append("\n")
				.append("Project ID: ").append(projectId).append("\n")
				.append("Project-isolated RAG documents: ").append(ragContexts.size()).append(" chunks\n")
				.append(ragContext);

			emitter.send(SseEmitter.event().name("start")
				.data(new DebateStreamStart(projectId, ragContexts.size(), agents, roundLimit)));

			List<DebateResponse.DebateTurn> turns = new ArrayList<>();
			boolean completed = false;
			int executedRounds = 0;

			String lastOracleOpinion = "";
			String lastBackendOpinion = "";
			String lastFrontendOpinion = "";
			String lastInspectorOpinion = "";

			for (int round = 1; round <= roundLimit; round++) {
				executedRounds = round;

				for (AiAgentType agent : agents) {
					String opinion = callAgent(agent, context, codeSnippet, projectId, ragContext, round, debateHistory);
					appendTurn(debateHistory, turns, round, agent, opinion);
					emitter.send(SseEmitter.event().name("turn").data(turns.get(turns.size() - 1)));

					switch (agent) {
						case ORACLE -> lastOracleOpinion = opinion;
						case BACKEND -> lastBackendOpinion = opinion;
						case FRONTEND -> lastFrontendOpinion = opinion;
						case INSPECTOR -> lastInspectorOpinion = opinion;
					}

					if (agent == AiAgentType.INSPECTOR && isDebateEndSignal(opinion)) {
						completed = true;
						break;
					}
				}

				if (completed) {
					break;
				}
			}

			String markdown = buildMarkdown(
				context,
				projectId,
				ragContexts.size(),
				completed,
				executedRounds,
				roundLimit,
				agents,
				debateHistory.toString(),
				lastOracleOpinion,
				lastBackendOpinion,
				lastFrontendOpinion,
				lastInspectorOpinion
			);

			DebateResponse response = new DebateResponse(
				projectId,
				context.fileName().trim(),
				context.cursorLine(),
				context.userQuery().trim(),
				completed,
				executedRounds,
				roundLimit,
				lastOracleOpinion,
				lastBackendOpinion,
				lastFrontendOpinion,
				lastInspectorOpinion,
				debateHistory.toString(),
				markdown,
				List.copyOf(ragContexts),
				List.copyOf(turns)
			);

			emitter.send(SseEmitter.event().name("done").data(response));
			emitter.complete();
		} catch (Exception exception) {
			log.warn("AI debate stream failed for projectId={}", projectId, exception);
			try {
				emitter.send(SseEmitter.event().name("error")
					.data(exception.getMessage() == null ? "AI debate stream failed." : exception.getMessage()));
			} catch (IOException ignored) {
				// 클라이언트가 이미 연결을 끊은 경우 - 무시하고 emitter만 정리한다.
			}
			emitter.completeWithError(exception);
		}
	}

	public record DebateStreamStart(Long projectId, int ragContextCount, List<AiAgentType> agents, int maxRounds) {
	}

	public SingleAgentResponse askAgent(User user, Long projectId, AiAgentType agent, EditorContextDto context) {
		validate(user, projectId, context);
		if (agent == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "agent is required.");
		}

		String codeSnippet = DebateContentGuard.sanitizeCodeSnippet(context.currentCodeSnippet().trim());
		List<String> ragContexts = projectRagRetriever.retrieve(projectId, buildRagQuery(context, codeSnippet), ThinkingLevel.from(context.level()));
		String ragContext = formatRagContext(projectId, ragContexts);
		StringBuilder debateHistory = new StringBuilder()
			.append("[Single Agent Request]\n")
			.append("User: ").append(user.getEmail()).append("\n")
			.append("Project ID: ").append(projectId).append("\n")
			.append("Selected agent: ").append(agent.displayName()).append("\n")
			.append("Project-isolated RAG documents: ").append(ragContexts.size()).append(" chunks\n")
			.append(ragContext);

		String answer = callAgent(agent, context, codeSnippet, projectId, ragContext, 1, debateHistory);
		String markdown = """
			# SYNAIPSE Single Agent Answer

			**Project ID:** `%d`
			**Agent:** `%s`
			**Role:** %s
			**Model:** `%s`
			**File:** `%s`
			**Cursor line:** `%d`
			**Question:** %s
			**RAG contexts:** %d project-filtered chunks

			## Answer
			%s
			""".formatted(
			projectId,
			agent.displayName(),
			agent.role(),
			agent.model(),
			context.fileName().trim(),
			context.cursorLine(),
			context.userQuery().trim(),
			ragContexts.size(),
			answer
		).trim();

		return new SingleAgentResponse(
			projectId,
			agent,
			agent.displayName(),
			agent.role(),
			agent.model(),
			context.fileName().trim(),
			context.cursorLine(),
			context.userQuery().trim(),
			answer,
			markdown,
			List.copyOf(ragContexts)
		);
	}

	private String callOracle(
		EditorContextDto context,
		String codeSnippet,
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		return requireResponse(
			"Oracle",
			AiCallRetrier.withRetry("Oracle debate", 2, 500, () -> oracleAi.debate(
				round,
				projectId,
				context.fileName(),
				codeSnippet,
				context.cursorLine(),
				context.userQuery(),
				ragContext,
				debateHistory.toString()
			))
		);
	}

	private String callBackend(
		EditorContextDto context,
		String codeSnippet,
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		return requireResponse(
			"Backend",
			AiCallRetrier.withRetry("Backend debate", 2, 500, () -> backendAi.debate(
				round,
				projectId,
				context.fileName(),
				codeSnippet,
				context.cursorLine(),
				context.userQuery(),
				ragContext,
				debateHistory.toString()
			))
		);
	}

	private String callFrontend(
		EditorContextDto context,
		String codeSnippet,
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		return requireResponse(
			"Frontend",
			AiCallRetrier.withRetry("Frontend debate", 2, 500, () -> frontendAi.debate(
				round,
				projectId,
				context.fileName(),
				codeSnippet,
				context.cursorLine(),
				context.userQuery(),
				ragContext,
				debateHistory.toString()
			))
		);
	}

	private String callInspector(
		EditorContextDto context,
		String codeSnippet,
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		return requireResponse(
			"Inspector",
			AiCallRetrier.withRetry("Inspector debate", 2, 500, () -> inspectorAi.debate(
				round,
				projectId,
				context.fileName(),
				codeSnippet,
				context.cursorLine(),
				context.userQuery(),
				ragContext,
				debateHistory.toString()
			))
		);
	}

	private String callAgent(
		AiAgentType agent,
		EditorContextDto context,
		String codeSnippet,
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		long startedAt = System.currentTimeMillis();
		try {
			String opinion = switch (agent) {
				case ORACLE -> callOracle(context, codeSnippet, projectId, ragContext, round, debateHistory);
				case BACKEND -> callBackend(context, codeSnippet, projectId, ragContext, round, debateHistory);
				case FRONTEND -> callFrontend(context, codeSnippet, projectId, ragContext, round, debateHistory);
				case INSPECTOR -> callInspector(context, codeSnippet, projectId, ragContext, round, debateHistory);
			};
			agentMetricsService.record(agent, projectId, true, System.currentTimeMillis() - startedAt, null);
			return opinion;
		} catch (RuntimeException exception) {
			agentMetricsService.record(agent, projectId, false, System.currentTimeMillis() - startedAt, exception.getMessage());
			throw exception;
		}
	}

	/**
	 * InspectorAi is instructed to put {@code DEBATE_END_KEYWORD} at the very end of its answer
	 * when the debate is resolved. Requiring it at the tail (rather than anywhere in the text)
	 * means a stray/injected occurrence of the keyword quoted mid-answer from the code snippet or
	 * a RAG document - which is neutralized before it ever reaches the model anyway, see
	 * {@link DebateContentGuard} - cannot be mistaken for a genuine completion signal.
	 */
	private boolean isDebateEndSignal(String opinion) {
		return opinion.stripTrailing().endsWith(DebateContentGuard.DEBATE_END_KEYWORD);
	}

	private void appendTurn(
		StringBuilder debateHistory,
		List<DebateResponse.DebateTurn> turns,
		int round,
		AiAgentType agent,
		String message
	) {
		debateHistory
			.append("\n\n[Round ").append(round).append(" / ").append(agent.displayName()).append(" / ").append(agent.model()).append("]\n")
			.append(message);
		turns.add(new DebateResponse.DebateTurn(round, agent.displayName(), agent.role(), agent.model(), message));
	}

	private List<AiAgentType> normalizeAgents(List<AiAgentType> selectedAgents) {
		if (selectedAgents == null || selectedAgents.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "agents must contain at least one agent.");
		}

		Set<AiAgentType> deduplicated = new LinkedHashSet<>();
		for (AiAgentType agent : selectedAgents) {
			if (agent == null) {
				throw new ApiException(ErrorCode.INVALID_INPUT, "agents cannot contain null.");
			}
			deduplicated.add(agent);
		}
		return List.copyOf(deduplicated);
	}

	private int normalizeMaxRounds(Integer requestedMaxRounds) {
		if (requestedMaxRounds == null) {
			return maxRounds;
		}
		if (requestedMaxRounds < 1 || requestedMaxRounds > 20) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "maxRounds must be between 1 and 20.");
		}
		return requestedMaxRounds;
	}

	/**
	 * Only InspectorAi's output is ever checked for the completion keyword (see
	 * {@link #isDebateEndSignal}), so a custom debate that omits INSPECTOR has no way to end early -
	 * it would silently burn every one of {@code maxRounds} rounds (defaulting to
	 * {@link #maxRounds}, currently 10) on every call. Requiring an explicit {@code maxRounds} in
	 * that case at least makes the cost a deliberate choice instead of a silent default.
	 */
	private void validateTerminationIsReachable(List<AiAgentType> agents, Integer requestedMaxRounds) {
		if (requestedMaxRounds == null && !agents.contains(AiAgentType.INSPECTOR)) {
			throw new ApiException(
				ErrorCode.INVALID_INPUT,
				"A custom debate without INSPECTOR cannot auto-detect completion. "
					+ "Include INSPECTOR in agents, or specify maxRounds explicitly."
			);
		}
	}

	private void validate(User user, Long projectId, EditorContextDto context) {
		if (user == null) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		if (projectId == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "projectId is required.");
		}
		if (context == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "editor context is required.");
		}
		if (!projectId.equals(context.projectId())) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "projectId must match the request body projectId.");
		}
		if (!StringUtils.hasText(context.fileName())) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "fileName is required.");
		}
		if (!StringUtils.hasText(context.currentCodeSnippet())) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "currentCodeSnippet is required.");
		}
		if (context.cursorLine() == null || context.cursorLine() < 1) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "cursorLine must be greater than or equal to 1.");
		}
		if (!StringUtils.hasText(context.userQuery())) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "userQuery is required.");
		}
	}

	private String buildRagQuery(EditorContextDto context, String codeSnippet) {
		return """
			File: %s
			Cursor line: %d
			Question: %s

			Code:
			%s
			""".formatted(
			context.fileName().trim(),
			context.cursorLine(),
			context.userQuery().trim(),
			codeSnippet
		);
	}

	private String formatRagContext(Long projectId, List<String> ragContexts) {
		if (ragContexts.isEmpty()) {
			return "CAUTION: There are not enough project samples for Project ID " + projectId
				+ ". Answer from the provided editor context and clearly disclose uncertainty.";
		}

		List<String> sanitizedChunks = ragContexts.stream()
			.map(DebateContentGuard::sanitizeFreeText)
			.toList();
		return RagContextSanitizer.wrap(projectId, sanitizedChunks);
	}

	private String requireResponse(String agent, String response) {
		if (!StringUtils.hasText(response)) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, agent + " returned an empty response.");
		}
		return response.trim();
	}

	private String buildMarkdown(
		EditorContextDto context,
		Long projectId,
		int ragContextCount,
		boolean completed,
		int executedRounds,
		int roundLimit,
		List<AiAgentType> agents,
		String debateHistory,
		String oracleOpinion,
		String backendOpinion,
		String frontendOpinion,
		String inspectorOpinion
	) {
		return """
			# SYNAIPSE Project-Isolated Debate

			**Project ID:** `%d`
			**File:** `%s`
			**Cursor line:** `%d`
			**Question:** %s
			**RAG contexts:** %d project-filtered chunks
			**Status:** %s
			**Rounds:** %d / %d
			**Selected agents:** %s

			## Latest Oracle Opinion
			%s

			## Latest Backend Opinion
			%s

			## Latest Frontend Opinion
			%s

			## Latest Inspector Opinion
			%s

			## Full Debate History
			%s
			""".formatted(
			projectId,
			context.fileName().trim(),
			context.cursorLine(),
			context.userQuery().trim(),
			ragContextCount,
			completed ? "COMPLETED_BY_INSPECTOR" : "MAX_ROUNDS_REACHED",
			executedRounds,
			roundLimit,
			formatSelectedAgents(agents),
			oracleOpinion,
			backendOpinion,
			frontendOpinion,
			inspectorOpinion,
			debateHistory
		).trim();
	}

	private String formatSelectedAgents(List<AiAgentType> agents) {
		return agents.stream()
			.map(agent -> "%s (`%s`)".formatted(agent.displayName(), agent.model()))
			.toList()
			.toString();
	}
}
