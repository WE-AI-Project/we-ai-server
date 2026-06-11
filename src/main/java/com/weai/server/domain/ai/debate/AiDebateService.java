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
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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
		ProjectRagRetriever projectRagRetriever,
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
		validate(user, projectId, context);

		List<String> ragContexts = projectRagRetriever.retrieve(projectId, buildRagQuery(context));
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

		for (int round = 1; round <= maxRounds; round++) {
			executedRounds = round;

			lastOracleOpinion = callOracle(context, projectId, ragContext, round, debateHistory);
			appendTurn(debateHistory, turns, round, "Oracle", "Chief coordinator", "llama3.1", lastOracleOpinion);

			lastBackendOpinion = callBackend(context, projectId, ragContext, round, debateHistory);
			appendTurn(debateHistory, turns, round, "Backend", "Server/API specialist", "qwen2.5-coder", lastBackendOpinion);

			lastFrontendOpinion = callFrontend(context, projectId, ragContext, round, debateHistory);
			appendTurn(debateHistory, turns, round, "Frontend", "UI/UX specialist", "llama3.1", lastFrontendOpinion);

			lastInspectorOpinion = callInspector(context, projectId, ragContext, round, debateHistory);
			appendTurn(debateHistory, turns, round, "Inspector", "QA/Security inspector", "qwen2.5-coder", lastInspectorOpinion);

			if (lastInspectorOpinion.contains(DEBATE_END_KEYWORD)) {
				completed = true;
				break;
			}
		}

		String markdown = buildMarkdown(
			context,
			projectId,
			ragContexts.size(),
			completed,
			executedRounds,
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
			maxRounds,
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

	private void appendTurn(
		StringBuilder debateHistory,
		List<DebateResponse.DebateTurn> turns,
		int round,
		String agent,
		String role,
		String model,
		String message
	) {
		debateHistory
			.append("\n\n[Round ").append(round).append(" / ").append(agent).append(" / ").append(model).append("]\n")
			.append(message);
		turns.add(new DebateResponse.DebateTurn(round, agent, role, model, message));
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
			return "No internal design documents were retrieved for Project ID: " + projectId + ".";
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
			**Models:** Oracle/Frontend = `llama3.1`, Backend/Inspector = `qwen2.5-coder`

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
			maxRounds,
			oracleOpinion,
			backendOpinion,
			frontendOpinion,
			inspectorOpinion,
			debateHistory
		).trim();
	}
}
