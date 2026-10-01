package com.weai.server.domain.ai.rag;

import com.weai.server.domain.ai.rag.WorkspaceRagIndexer.WorkspaceIndexStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "What is currently indexed in a project's RAG store")
public record RagDocumentListResponse(
	Long projectId,
	int documentCount,
	int totalChunks,
	List<Item> documents,
	@Schema(description = "Latest workspace auto-indexing run since the server started (null if none)")
	WorkspaceIndexStatus workspaceIndexing
) {
	public static RagDocumentListResponse of(Long projectId, List<RagDocument> documents, WorkspaceIndexStatus status) {
		List<Item> items = documents.stream().map(Item::from).toList();
		int totalChunks = items.stream().mapToInt(Item::chunkCount).sum();
		return new RagDocumentListResponse(projectId, items.size(), totalChunks, items, status);
	}

	public record Item(String source, String origin, int chunkCount, String embeddingModel, LocalDateTime indexedAt) {
		static Item from(RagDocument document) {
			return new Item(
				document.getSource(),
				document.getOrigin().name(),
				document.getChunkCount(),
				document.getEmbeddingModel(),
				document.getUpdatedAt()
			);
		}
	}
}
