package com.weai.server.domain.ai.rag;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "RAG document indexing result")
public record RagDocumentIndexResponse(
	@Schema(description = "Project id used for RAG isolation", example = "1")
	Long projectId,

	@Schema(description = "Indexed source name or path", example = "docs/backend/auth.md")
	String source,

	@Schema(description = "Number of chunks indexed", example = "7")
	int chunkCount,

	@ArraySchema(schema = @Schema(description = "Embedding store id for an indexed chunk"))
	List<String> embeddingIds
) {
}
