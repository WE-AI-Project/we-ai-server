package com.weai.server.domain.ai.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
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
 *
 * Retrieval is hybrid: a wide, low-threshold dense search over the project's chunks is re-ranked
 * by {@link HybridRelevanceScorer}, because nomic-embed-text alone kept returning the same
 * unrelated CSS chunks for Korean questions whose answer was literally in an indexed document.
 */
@Slf4j
@Lazy
@Component
public class ProjectRagRetriever {

	// 재정렬 후보 풀: 최종 결과 수의 5배(최소 20개)를 낮은 기준 점수로 먼저 넉넉하게 가져온다.
	private static final int MIN_CANDIDATE_POOL = 20;
	private static final int CANDIDATE_POOL_MULTIPLIER = 5;
	private static final double CANDIDATE_MIN_SCORE = 0.3;

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
		String trimmedQuery = query.trim();
		Filter projectFilter = metadataKey("projectId").isEqualTo(projectId);

		try {
			Embedding queryEmbedding = embeddingModel.embed(trimmedQuery).content();
			EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
				.queryEmbedding(queryEmbedding)
				.maxResults(Math.max(MIN_CANDIDATE_POOL, effectiveLevel.maxResults() * CANDIDATE_POOL_MULTIPLIER))
				.minScore(CANDIDATE_MIN_SCORE)
				.filter(projectFilter)
				.build();

			List<HybridRelevanceScorer.Candidate> candidates = embeddingStore.search(searchRequest)
				.matches()
				.stream()
				.filter(match -> match.embedded() != null && StringUtils.hasText(match.embedded().text()))
				.map(match -> new HybridRelevanceScorer.Candidate(match.embedded().text(), scoreOf(match)))
				.toList();

			// 최종 컷(벡터 minScore 또는 키워드 겹침)과 개수 제한은 ThinkingLevel 기준 그대로 유지한다.
			return HybridRelevanceScorer.rank(trimmedQuery, candidates, effectiveLevel.minScore(), effectiveLevel.maxResults())
				.stream()
				.map(HybridRelevanceScorer.ScoredCandidate::text)
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

	private static double scoreOf(EmbeddingMatch<TextSegment> match) {
		return match.score() == null ? 0.0 : match.score();
	}
}
