package com.weai.server.domain.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Project document indexing request for RAG")
public record RagDocumentIndexRequest(
	@Schema(description = "Workspace/project id used to isolate RAG retrieval", example = "1")
	@NotNull(message = "projectId is required.")
	Long projectId,

	@Schema(description = "Document source name or path", example = "docs/backend/auth.md")
	@NotBlank(message = "source is required.")
	@Size(max = 1000, message = "source must be 1000 characters or fewer.")
	String source,

	@Schema(description = "Plain text content to chunk, embed, and index")
	@NotBlank(message = "text is required.")
	@Size(max = 1000000, message = "text must be 1000000 characters or fewer.")
	String text
) {
}
