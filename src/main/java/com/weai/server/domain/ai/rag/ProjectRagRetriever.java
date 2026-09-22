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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

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

		return retriever.retrieve(Query.from(query.trim()))
			.stream()
			.map(Content::textSegment)
			.map(TextSegment::text)
			.filter(StringUtils::hasText)
			.toList();
	}
}
