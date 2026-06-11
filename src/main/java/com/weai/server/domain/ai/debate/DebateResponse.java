package com.weai.server.domain.ai.debate;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Dynamic turn-based SYNAIPSE debate result")
public record DebateResponse(
	@Schema(description = "Workspace/project id used for authorization and RAG isolation", example = "1")
	Long projectId,

	@Schema(description = "Original file name from the editor", example = "src/app/components/ChatPage.tsx")
	String fileName,

	@Schema(description = "Original cursor line", example = "42")
	Integer cursorLine,

	@Schema(description = "Original developer question", example = "Why does this component re-render too often?")
	String userQuery,

	@Schema(description = "Whether InspectorAi ended the debate with the keyword", example = "true")
	boolean completed,

	@Schema(description = "Executed debate rounds", example = "3")
	int executedRounds,

	@Schema(description = "Configured max debate rounds", example = "10")
	int maxRounds,

	@Schema(description = "Latest Oracle opinion")
	String oracleAnalysis,

	@Schema(description = "Latest Backend specialist opinion")
	String backendOpinion,

	@Schema(description = "Latest Frontend specialist opinion")
	String frontendOpinion,

	@Schema(description = "Latest Inspector QA and security review")
	String inspectorOpinion,

	@Schema(description = "Full shared debate history accumulated by StringBuilder")
	String debateHistory,

	@Schema(description = "Markdown-formatted full debate result")
	String markdown,

	@ArraySchema(schema = @Schema(description = "Project-isolated RAG contexts used for this debate"))
	List<String> ragContexts,

	@ArraySchema(schema = @Schema(implementation = DebateTurn.class))
	List<DebateTurn> turns
) {

	@Schema(description = "One agent turn in the debate")
	public record DebateTurn(
		@Schema(description = "Round number", example = "1")
		int round,

		@Schema(description = "Agent name", example = "Oracle")
		String agent,

		@Schema(description = "Agent role", example = "Chief coordinator")
		String role,

		@Schema(description = "Ollama model used for this turn", example = "llama3.1")
		String model,

		@Schema(description = "Generated message")
		String message
	) {
	}
}
