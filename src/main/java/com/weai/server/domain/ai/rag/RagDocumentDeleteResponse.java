package com.weai.server.domain.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of removing a document's previously indexed RAG chunks")
public record RagDocumentDeleteResponse(
	@Schema(description = "Workspace/project id", example = "1")
	Long projectId,

	@Schema(description = "Document source name or path that was removed", example = "docs/backend/auth.md")
	String source
) {
}
