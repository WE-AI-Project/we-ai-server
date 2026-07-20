package com.weai.server.domain.ai.debate;

import com.weai.server.domain.ai.debate.agent.BackendAi;
import com.weai.server.domain.ai.debate.agent.FrontendAi;
import com.weai.server.domain.ai.debate.agent.InspectorAi;
import com.weai.server.domain.ai.debate.agent.OracleAi;
import com.weai.server.domain.ai.rag.ProjectRagRetriever;
import com.weai.server.domain.user.domain.User;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiDebateService {

	private static final String DEBATE_END_KEYWORD = "[토론 종료]";

	private final OracleAi oracleAi;
	private final BackendAi backendAi;
	private final FrontendAi frontendAi;
	private final InspectorAi inspectorAi;
	private final ProjectRagRetriever projectRagRetriever;
	private final int maxRounds;

	public AiDebateService(
		OracleAi oracleAi,
		BackendAi backendAi,
		FrontendAi frontendAi,
		InspectorAi inspectorAi,
		@Lazy ProjectRagRetriever projectRagRetriever,
		@Value("${ai.debate.max-rounds:10}") int maxRounds
	) {
		this.oracleAi = oracleAi;
		this.backendAi = backendAi;
		this.frontendAi = frontendAi;
		this.inspectorAi = inspectorAi;
		this.projectRagRetriever = projectRagRetriever;
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

		List<String> ragContexts = projectRagRetriever.retrieve(projectId, buildRagQuery(context), context.ragMaxResults());
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
				String opinion = callAgent(agent, context, projectId, ragContext, round, debateHistory);
				appendTurn(debateHistory, turns, round, agent, opinion);

				switch (agent) {
					case ORACLE -> lastOracleOpinion = opinion;
					case BACKEND -> lastBackendOpinion = opinion;
					case FRONTEND -> lastFrontendOpinion = opinion;
					case INSPECTOR -> lastInspectorOpinion = opinion;
				}

				if (agent == AiAgentType.INSPECTOR && opinion.contains(DEBATE_END_KEYWORD)) {
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

	public SingleAgentResponse askAgent(User user, Long projectId, AiAgentType agent, EditorContextDto context) {
		validate(user, projectId, context);
		if (agent == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "agent is required.");
		}

		List<String> ragContexts = projectRagRetriever.retrieve(projectId, buildRagQuery(context), context.ragMaxResults());
		String ragContext = formatRagContext(projectId, ragContexts);
		StringBuilder debateHistory = new StringBuilder()
			.append("[Single Agent Request]\n")
			.append("User: ").append(user.getEmail()).append("\n")
			.append("Project ID: ").append(projectId).append("\n")
			.append("Selected agent: ").append(agent.displayName()).append("\n")
			.append("Project-isolated RAG documents: ").append(ragContexts.size()).append(" chunks\n")
			.append(ragContext);

		String answer = callAgent(agent, context, projectId, ragContext, 1, debateHistory);
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
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		return requireResponse(
			"Oracle",
			oracleAi.debate(
				round,
				projectId,
				context.fileName(),
				context.currentCodeSnippet(),
				context.cursorLine(),
				context.userQuery(),
				ragContext,
				debateHistory.toString()
			)
		);
	}

	private String callBackend(
		EditorContextDto context,
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		return requireResponse(
			"Backend",
			backendAi.debate(
				round,
				projectId,
				context.fileName(),
				context.currentCodeSnippet(),
				context.cursorLine(),
				context.userQuery(),
				ragContext,
				debateHistory.toString()
			)
		);
	}

	private String callFrontend(
		EditorContextDto context,
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		return requireResponse(
			"Frontend",
			frontendAi.debate(
				round,
				projectId,
				context.fileName(),
				context.currentCodeSnippet(),
				context.cursorLine(),
				context.userQuery(),
				ragContext,
				debateHistory.toString()
			)
		);
	}

	private String callInspector(
		EditorContextDto context,
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		return requireResponse(
			"Inspector",
			inspectorAi.debate(
				round,
				projectId,
				context.fileName(),
				context.currentCodeSnippet(),
				context.cursorLine(),
				context.userQuery(),
				ragContext,
				debateHistory.toString()
			)
		);
	}

	private String callAgent(
		AiAgentType agent,
		EditorContextDto context,
		Long projectId,
		String ragContext,
		int round,
		StringBuilder debateHistory
	) {
		return switch (agent) {
			case ORACLE -> callOracle(context, projectId, ragContext, round, debateHistory);
			case BACKEND -> callBackend(context, projectId, ragContext, round, debateHistory);
			case FRONTEND -> callFrontend(context, projectId, ragContext, round, debateHistory);
			case INSPECTOR -> callInspector(context, projectId, ragContext, round, debateHistory);
		};
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

	private String buildRagQuery(EditorContextDto context) {
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
			context.currentCodeSnippet().trim()
		);
	}

	private String formatRagContext(Long projectId, List<String> ragContexts) {
		if (ragContexts.isEmpty()) {
			return "CAUTION: There are not enough project samples for Project ID " + projectId
				+ ". Answer from the provided editor context and clearly disclose uncertainty.";
		}

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < ragContexts.size(); i++) {
			builder
				.append("\n\n[Project ")
				.append(projectId)
				.append(" / RAG Document ")
				.append(i + 1)
				.append("]\n")
				.append(ragContexts.get(i));
		}
		return builder.toString();
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
