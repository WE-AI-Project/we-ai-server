package com.weai.server.domain.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class HybridRelevanceScorerTest {

	private static final String PLAN_CHUNK = """
		## 역할 분담
		| 이름 | 역할 | 담당 업무 |
		| --- | --- | --- |
		| 김민혁 | 백엔드/서버 | 로그인·프로젝트 생성 API, 배포 |
		| 이서연 | 프론트엔드 | 메인 화면, 헤더 메뉴 |
		""";
	private static final String TOKENS_CSS = ":root { --color-primary: #3b82f6; --radius-md: 8px; --font-body: 'Pretendard'; }";
	private static final String GLOBAL_CSS = "body { margin: 0; font-family: var(--font-body); } .container { max-width: 1200px; }";
	private static final String HEADER_JSX = "export default function Header() { return <nav className=\"menu\"><a href=\"/\">홈</a><a href=\"/about\">소개</a></nav>; }";

	@Test
	void koreanRoleQuestionScoresTheRoleTableAboveCssChunks() {
		String query = "백엔드 담당자 누구야";

		double plan = HybridRelevanceScorer.lexicalScore(query, PLAN_CHUNK);

		// 백엔/엔드/담당 3개가 겹치고 "담당자" 의 "당자" 만 빠진다. "누구야" 는 의문사라 분모에서 빠진다.
		assertThat(plan).isEqualTo(0.75);
		assertThat(HybridRelevanceScorer.lexicalScore(query, TOKENS_CSS)).isZero();
		assertThat(HybridRelevanceScorer.lexicalScore(query, GLOBAL_CSS)).isZero();
		assertThat(HybridRelevanceScorer.lexicalScore(query, HEADER_JSX)).isZero();
	}

	@Test
	void rankPutsTheRoleTableFirstEvenWhenCssHasTheHigherVectorScore() {
		List<HybridRelevanceScorer.ScoredCandidate> ranked = HybridRelevanceScorer.rank(
			"백엔드 담당자 누구야?",
			List.of(
				new HybridRelevanceScorer.Candidate(TOKENS_CSS, 0.80),
				new HybridRelevanceScorer.Candidate(HEADER_JSX, 0.78),
				new HybridRelevanceScorer.Candidate(GLOBAL_CSS, 0.77),
				new HybridRelevanceScorer.Candidate(PLAN_CHUNK, 0.60)
			),
			ThinkingLevel.DEFAULT.minScore(),
			ThinkingLevel.DEFAULT.maxResults()
		);

		assertThat(ranked).hasSize(4);
		assertThat(ranked.get(0).text()).isEqualTo(PLAN_CHUNK);
	}

	@Test
	void keepsLowVectorCandidatesOnlyWhenTheyMatchLexically() {
		List<HybridRelevanceScorer.ScoredCandidate> ranked = HybridRelevanceScorer.rank(
			"역할 분담 김민혁",
			List.of(
				new HybridRelevanceScorer.Candidate(TOKENS_CSS, 0.40),
				new HybridRelevanceScorer.Candidate(PLAN_CHUNK, 0.40)
			),
			0.65,
			4
		);

		assertThat(ranked).extracting(HybridRelevanceScorer.ScoredCandidate::text).containsExactly(PLAN_CHUNK);
	}

	@Test
	void latinWordsMatchWholeWordsOnly() {
		assertThat(HybridRelevanceScorer.lexicalScore("Who owns the API?", "The api module is owned by the backend team")).isEqualTo(0.5);
		// "api" 는 "rapid" 안의 부분 문자열로는 매칭되지 않는다.
		assertThat(HybridRelevanceScorer.lexicalScore("api", "rapid prototyping")).isZero();
	}

	@Test
	void particlesAreStrippedFromQueryTokens() {
		assertThat(HybridRelevanceScorer.lexicalScore("김민혁이 맡은 일", "| 김민혁 | 백엔드 |"))
			.isEqualTo(HybridRelevanceScorer.lexicalScore("김민혁 맡은 일", "| 김민혁 | 백엔드 |"));
	}

	@Test
	void rankingIsDeterministicForTies() {
		List<HybridRelevanceScorer.Candidate> candidates = List.of(
			new HybridRelevanceScorer.Candidate("first", 0.7),
			new HybridRelevanceScorer.Candidate("second", 0.7),
			new HybridRelevanceScorer.Candidate("third", 0.7)
		);

		for (int i = 0; i < 5; i++) {
			assertThat(HybridRelevanceScorer.rank("unrelated", candidates, 0.65, 4))
				.extracting(HybridRelevanceScorer.ScoredCandidate::text)
				.containsExactly("first", "second", "third");
		}
	}
}
