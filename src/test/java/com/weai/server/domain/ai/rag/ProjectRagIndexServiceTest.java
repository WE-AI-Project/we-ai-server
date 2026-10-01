package com.weai.server.domain.ai.rag;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weai.server.domain.ai.rag.RagTestSupport.CountingEmbeddingModel;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectRagIndexServiceTest {

	private static final Long PROJECT_ID = 7L;

	private InMemoryEmbeddingStore<TextSegment> store;
	private CountingEmbeddingModel embeddingModel;
	private RagDocumentRepository repository;
	private ProjectRagIndexService service;

	@BeforeEach
	void setUp() {
		store = new InMemoryEmbeddingStore<>();
		embeddingModel = new CountingEmbeddingModel();
		repository = RagTestSupport.inMemoryRepository();
		service = new ProjectRagIndexService(store, embeddingModel, repository, "test-embed");
	}

	@Test
	void reindexingIdenticalContentSkipsEmbedding() {
		RagDocumentIndexResponse first = service.index(PROJECT_ID, "docs/auth.md", "JWT tokens are validated per request.");
		RagDocumentIndexResponse second = service.index(PROJECT_ID, "docs/auth.md", "JWT tokens are validated per request.");

		assertThat(first.unchanged()).isFalse();
		assertThat(second.unchanged()).isTrue();
		assertThat(second.chunkCount()).isEqualTo(first.chunkCount());
		assertThat(embeddingModel.embeddedSegments.get()).isEqualTo(first.chunkCount());
	}

	@Test
	void changedContentReplacesPreviousChunksInsteadOfAppending() {
		service.index(PROJECT_ID, "docs/auth.md", "version one");
		service.index(PROJECT_ID, "docs/auth.md", "version two");

		assertThat(chunkCount("docs/auth.md")).isEqualTo(1);
		assertThat(repository.findByProjectIdAndSource(PROJECT_ID, "docs/auth.md")).isPresent();
	}

	@Test
	void rejectsSecretFiles() {
		assertThatThrownBy(() -> service.index(PROJECT_ID, ".env", "DB_PASSWORD=hunter2"))
			.isInstanceOf(ApiException.class);
		assertThat(embeddingModel.embeddedSegments.get()).isZero();
	}

	@Test
	void deleteRemovesChunksAndRegistryEntry() {
		service.index(PROJECT_ID, "docs/auth.md", "some text");

		service.delete(PROJECT_ID, "docs/auth.md");

		assertThat(chunkCount("docs/auth.md")).isZero();
		assertThat(service.list(PROJECT_ID)).isEmpty();
	}

	private int chunkCount(String source) {
		return store.search(EmbeddingSearchRequest.builder()
				.queryEmbedding(Embedding.from(new float[] {1f, 1f, 1f}))
				.maxResults(100)
				.minScore(0.0)
				.filter(metadataKey("projectId").isEqualTo(PROJECT_ID).and(metadataKey("source").isEqualTo(source)))
				.build())
			.matches()
			.size();
	}
}
