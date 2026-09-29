package com.weai.server.domain.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to remove every previously indexed RAG chunk for one document source")
public record RagDocumentDeleteRequest(
	@Schema(description = "Workspace/project id used to isolate RAG retrieval", example = "1")
	@NotNull(message = "projectId is required.")
	Long projectId,

	@Schema(description = "Document source name or path, exactly as it was indexed", example = "docs/backend/auth.md")
	@NotBlank(message = "source is required.")
	@Size(max = 1000, message = "source must be 1000 characters or fewer.")
	String source
) {
}
