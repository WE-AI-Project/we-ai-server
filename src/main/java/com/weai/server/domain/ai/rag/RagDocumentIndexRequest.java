package com.weai.server.domain.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Project document indexing request for RAG")
public record RagDocumentIndexRequest(
	@Schema(description = "Workspace/project id used to isolate RAG retrieval", example = "1")
	@NotNull(message = "projectId is required.")
	Long projectId,

	@Schema(description = "Document source name or path", example = "docs/backend/auth.md")
	@NotBlank(message = "source is required.")
	String source,

	@Schema(description = "Plain text content to chunk, embed, and index")
	@NotBlank(message = "text is required.")
	String text
) {
}
