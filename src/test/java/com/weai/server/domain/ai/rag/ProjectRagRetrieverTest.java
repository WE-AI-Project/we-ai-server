package com.weai.server.domain.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reproduces the production failure (project 23): dense similarity alone prefers CSS/JSX chunks
 * for "백엔드 담당자 누구야?", so the role table in docs/PLAN.md never made the top results.
 */
class ProjectRagRetrieverTest {

	private static final String QUERY = "백엔드 담당자 누구야?";
	private static final String PLAN_CHUNK = """
		## 역할 분담
		| 이름 | 역할 | 담당 업무 |
		| 김민혁 | 백엔드/서버 | 로그인·프로젝트 생성 API, 배포 |
		""";
	private static final List<String> CSS_CHUNKS = List.of(
		":root { --color-primary: #3b82f6; --radius-md: 8px; }",
		"export default function Header() { return <nav className=\"menu\">홈 소개</nav>; }",
		"body { margin: 0; font-family: var(--font-body); }",
		".container { max-width: 1200px; padding: 0 16px; }"
	);
	private static final String OTHER_PROJECT_CHUNK = "다른 프로젝트: 백엔드 담당자는 박지훈";

	private InMemoryEmbeddingStore<TextSegment> store;
	private ProjectRagRetriever retriever;

	@BeforeEach
	void setUp() {
		store = new InMemoryEmbeddingStore<>();
		EmbeddingModel model = new FakeEmbeddingModel();
		retriever = new ProjectRagRetriever(store, model);

		for (String css : CSS_CHUNKS) {
			add(1L, css, model);
		}
		add(1L, PLAN_CHUNK, model);
		add(2L, OTHER_PROJECT_CHUNK, model);
	}

	@Test
	void hybridRerankingPutsThePlanChunkFirstDespiteLowerVectorSimilarity() {
		List<String> results = retriever.retrieve(1L, QUERY, ThinkingLevel.DEFAULT);

		assertThat(results).hasSize(ThinkingLevel.DEFAULT.maxResults());
		assertThat(results.get(0)).isEqualTo(PLAN_CHUNK);
	}

	@Test
	void projectFilterStillIsolatesProjects() {
		List<String> projectOne = retriever.retrieve(1L, QUERY, ThinkingLevel.HIGH);
		List<String> projectTwo = retriever.retrieve(2L, QUERY, ThinkingLevel.HIGH);

		// 프로젝트 2 청크는 벡터/키워드 모두 가장 높지만 프로젝트 1 결과에는 절대 섞이지 않는다.
		assertThat(projectOne).doesNotContain(OTHER_PROJECT_CHUNK).contains(PLAN_CHUNK);
		assertThat(projectTwo).containsExactly(OTHER_PROJECT_CHUNK);
	}

	@Test
	void embeddingFailuresDegradeToNoContext() {
		ProjectRagRetriever broken = new ProjectRagRetriever(store, new EmbeddingModel() {
			@Override
			public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
				throw new IllegalStateException("embedding model down");
			}
		});

		assertThat(broken.retrieve(1L, QUERY)).isEmpty();
	}

	private void add(Long projectId, String text, EmbeddingModel model) {
		TextSegment segment = TextSegment.from(text, new Metadata().put("projectId", projectId));
		store.add(model.embed(segment).content(), segment);
	}

	/**
	 * Query vector is (1, 0). CSS/JSX chunks sit at cosine 0.6 (relevance 0.80), the PLAN chunk at
	 * cosine 0.24 (relevance 0.62, below DEFAULT minScore 0.65) - i.e. dense retrieval alone would
	 * drop the only chunk that answers the question. The other project's chunk is a perfect match.
	 */
	private static final class FakeEmbeddingModel implements EmbeddingModel {

		@Override
		public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
			return Response.from(segments.stream().map(segment -> Embedding.from(vectorFor(segment.text()))).toList());
		}

		private static float[] vectorFor(String text) {
			if (text.equals(QUERY) || text.equals(OTHER_PROJECT_CHUNK)) {
				return new float[] {1f, 0f};
			}
			if (text.equals(PLAN_CHUNK)) {
				return new float[] {0.24f, 0.9708f};
			}
			return new float[] {0.6f, 0.8f};
		}
	}
}
