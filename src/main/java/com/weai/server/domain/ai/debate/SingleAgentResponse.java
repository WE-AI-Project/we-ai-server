package com.weai.server.domain.ai.debate;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Single-agent RAG-grounded answer")
public record SingleAgentResponse(
	@Schema(description = "Workspace/project id used for authorization and RAG isolation", example = "1")
	Long projectId,

	@Schema(description = "Agent that answered this request", example = "BACKEND")
	AiAgentType agent,

	@Schema(description = "Human-readable agent name", example = "Backend")
	String agentName,

	@Schema(description = "Agent role", example = "Server/API specialist")
	String role,

	@Schema(description = "Model backing this agent", example = "qwen2.5-coder")
	String model,

	@Schema(description = "Original file name from the editor", example = "src/main/java/App.java")
	String fileName,

	@Schema(description = "Original cursor line", example = "42")
	Integer cursorLine,

	@Schema(description = "Original developer question")
	String userQuery,

	@Schema(description = "Generated answer")
	String answer,

	@Schema(description = "Markdown-formatted answer")
	String markdown,

	@ArraySchema(schema = @Schema(description = "Project-isolated RAG contexts used for this answer"))
	List<String> ragContexts
) {
}
