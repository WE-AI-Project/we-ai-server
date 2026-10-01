package com.weai.server.domain.ai.rag;

import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registry of what is currently indexed in the vector store, per (project, source). Lets indexing
 * skip unchanged content, lets workspace re-uploads drop files that no longer exist, and lets
 * clients list a project's RAG contents without querying Chroma.
 */
@Getter
@Entity
@Table(name = "rag_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RagDocument extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(nullable = false, length = 500)
	private String source;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RagDocumentOrigin origin;

	@Column(name = "chunk_count", nullable = false)
	private int chunkCount;

	@Column(name = "content_hash", nullable = false, length = 64)
	private String contentHash;

	@Column(name = "embedding_model", nullable = false, length = 100)
	private String embeddingModel;

	public static RagDocument create(Long projectId, String source) {
		RagDocument document = new RagDocument();
		document.projectId = projectId;
		document.source = source;
		return document;
	}

	public void markIndexed(RagDocumentOrigin origin, int chunkCount, String contentHash, String embeddingModel) {
		this.origin = origin;
		this.chunkCount = chunkCount;
		this.contentHash = contentHash;
		this.embeddingModel = embeddingModel;
	}

	public boolean isUpToDate(String contentHash, String embeddingModel) {
		return this.contentHash.equals(contentHash) && this.embeddingModel.equals(embeddingModel);
	}
}
