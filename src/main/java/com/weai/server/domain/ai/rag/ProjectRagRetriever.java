package com.weai.server.domain.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Every caller (chat, QA, commit, debate, syn-commit) already knows how to degrade gracefully when
 * no RAG context is found - they fall back to general knowledge / diff-only analysis instead of
 * refusing. Before this class also treated ChromaDB/embedding-model connectivity failures as "no
 * context" (rather than letting the exception propagate), an outage there turned into a 500 on
 * every AI endpoint instead of that same graceful degradation.
 */
@Slf4j
@Lazy
@Component
public class ProjectRagRetriever {

	private final EmbeddingStore<TextSegment> embeddingStore;
	private final EmbeddingModel embeddingModel;

	public ProjectRagRetriever(
		@Qualifier("oracleChromaEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore,
		@Qualifier("oracleEmbeddingModel") EmbeddingModel embeddingModel
	) {
		this.embeddingStore = embeddingStore;
		this.embeddingModel = embeddingModel;
	}

	public List<String> retrieve(Long projectId, String query) {
		return retrieve(projectId, query, ThinkingLevel.DEFAULT);
	}

	public List<String> retrieve(Long projectId, String query, ThinkingLevel level) {
		if (projectId == null || !StringUtils.hasText(query)) {
			return List.of();
		}

		ThinkingLevel effectiveLevel = level == null ? ThinkingLevel.DEFAULT : level;
		Filter projectFilter = metadataKey("projectId").isEqualTo(projectId);
		ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
			.embeddingStore(embeddingStore)
			.embeddingModel(embeddingModel)
			.maxResults(effectiveLevel.maxResults())
			.minScore(effectiveLevel.minScore())
			.filter(projectFilter)
			.build();

		try {
			return retriever.retrieve(Query.from(query.trim()))
				.stream()
				.map(Content::textSegment)
				.map(TextSegment::text)
				.filter(StringUtils::hasText)
				.toList();
		} catch (RuntimeException exception) {
			log.warn(
				"RAG retrieval failed for projectId={} (embedding store/model unavailable?); "
					+ "degrading to no context instead of failing the request.",
				projectId, exception
			);
			return List.of();
		}
	}
}
