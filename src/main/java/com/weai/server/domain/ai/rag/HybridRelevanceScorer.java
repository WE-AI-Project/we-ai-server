package com.weai.server.domain.ai.rag;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Re-ranks dense-retrieval candidates with a cheap, tokenizer-free lexical signal.
 *
 * nomic-embed-text 는 한국어 질문을 거의 구분하지 못해서 ("백엔드 담당자 누구야?" 와
 * "역할 분담 김민혁" 이 똑같이 CSS 청크 4개를 돌려줬다), 벡터 점수만으로는 정답 청크가
 * 상위 N개에 들어오지 못한다. 형태소 분석기 없이도 한국어에서 잘 동작하는 문자 bigram 겹침과
 * 라틴 문자 단어 단위 겹침을 섞어 벡터 점수를 보정한다. 입력이 같으면 항상 같은 순서를 돌려준다.
 */
final class HybridRelevanceScorer {

	static final double VECTOR_WEIGHT = 0.6;
	static final double LEXICAL_WEIGHT = 0.4;
	// 벡터 점수가 ThinkingLevel.minScore 미만이어도 질문 키워드의 절반 이상이 청크에 있으면 살린다.
	static final double LEXICAL_KEEP_THRESHOLD = 0.5;

	// 한글 연속 구간과 그 외 문자/숫자 연속 구간을 따로 잘라서 "API를" 같은 혼합 토큰도 분리한다.
	private static final Pattern TOKEN_PATTERN = Pattern.compile("\\p{IsHangul}+|[\\p{L}\\p{N}&&[^\\p{IsHangul}]]+");
	private static final Pattern HANGUL_PATTERN = Pattern.compile("\\p{IsHangul}+");

	// 질문에 거의 항상 붙는 의문사/요청 표현 - 문서 쪽에는 나오지 않으니 분모만 키운다.
	private static final Set<String> KOREAN_STOPWORDS = Set.of(
		"누구", "누구야", "누구예요", "누구인가요", "누가", "무엇", "뭐", "뭐야", "뭔가요", "어디", "어디야",
		"언제", "어떻게", "왜", "알려줘", "알려주세요", "있어", "있나요", "있어요", "해줘", "해주세요",
		"인가요", "입니다", "이야", "그리고", "좀"
	);
	private static final Set<String> ENGLISH_STOPWORDS = Set.of(
		"a", "an", "the", "is", "are", "was", "were", "be", "who", "what", "which", "where", "when",
		"how", "why", "do", "does", "for", "of", "to", "in", "on", "and", "or", "it", "this", "that",
		"me", "please", "tell", "with"
	);
	// 길이가 긴 조사부터 확인해야 "에서" 가 "서" 보다 먼저 잘린다.
	private static final List<String> KOREAN_PARTICLES = List.of(
		"에서", "으로", "에게", "한테", "까지", "부터", "은", "는", "이", "가", "을", "를", "의", "에",
		"와", "과", "도", "로", "야", "요"
	);

	private HybridRelevanceScorer() {
	}

	record Candidate(String text, double vectorScore) {
	}

	record ScoredCandidate(String text, double vectorScore, double lexicalScore, double finalScore) {
	}

	/**
	 * Keeps a candidate when its vector score reaches {@code minVectorScore} or its lexical score
	 * reaches {@link #LEXICAL_KEEP_THRESHOLD}, then orders by
	 * {@code VECTOR_WEIGHT * vector + LEXICAL_WEIGHT * lexical} (ties: higher vector score, then
	 * original order) and returns at most {@code maxResults}.
	 */
	static List<ScoredCandidate> rank(String query, List<Candidate> candidates, double minVectorScore, int maxResults) {
		QueryTerms queryTerms = QueryTerms.of(query);
		List<ScoredCandidate> kept = new ArrayList<>();
		for (Candidate candidate : candidates) {
			double lexical = lexicalScore(queryTerms, candidate.text());
			if (candidate.vectorScore() < minVectorScore && lexical < LEXICAL_KEEP_THRESHOLD) {
				continue;
			}
			double combined = VECTOR_WEIGHT * candidate.vectorScore() + LEXICAL_WEIGHT * lexical;
			kept.add(new ScoredCandidate(candidate.text(), candidate.vectorScore(), lexical, combined));
		}

		// List.sort 는 안정 정렬이라 동점이면 임베딩 스토어가 준 원래 순서가 유지된다.
		kept.sort(Comparator.comparingDouble(ScoredCandidate::finalScore).reversed()
			.thenComparing(Comparator.comparingDouble(ScoredCandidate::vectorScore).reversed()));
		return kept.size() > maxResults ? List.copyOf(kept.subList(0, maxResults)) : List.copyOf(kept);
	}

	static double lexicalScore(String query, String text) {
		return lexicalScore(QueryTerms.of(query), text);
	}

	/**
	 * (matched query bigrams + matched query words) / (query bigrams + query words), in [0, 1].
	 * Hangul runs contribute character bigrams, every other letter/digit run contributes one whole
	 * word, so Korean needs no tokenizer and "api" does not match inside "rapid".
	 */
	private static double lexicalScore(QueryTerms queryTerms, String text) {
		int total = queryTerms.bigrams().size() + queryTerms.words().size();
		if (total == 0 || text == null || text.isBlank()) {
			return 0.0;
		}

		Set<String> textBigrams = new HashSet<>();
		Set<String> textWords = new HashSet<>();
		for (String token : tokenize(text)) {
			if (isHangul(token)) {
				addBigrams(token, textBigrams);
			} else {
				textWords.add(token);
			}
		}

		int matched = 0;
		for (String bigram : queryTerms.bigrams()) {
			if (textBigrams.contains(bigram)) {
				matched++;
			}
		}
		for (String word : queryTerms.words()) {
			if (textWords.contains(word)) {
				matched++;
			}
		}
		return (double) matched / total;
	}

	private record QueryTerms(Set<String> bigrams, Set<String> words) {

		static QueryTerms of(String query) {
			Set<String> bigrams = new HashSet<>();
			Set<String> words = new HashSet<>();
			if (query == null) {
				return new QueryTerms(bigrams, words);
			}
			for (String token : tokenize(query)) {
				if (isHangul(token)) {
					if (KOREAN_STOPWORDS.contains(token)) {
						continue;
					}
					addBigrams(stripParticle(token), bigrams);
				} else if (token.length() >= 2 && !ENGLISH_STOPWORDS.contains(token)) {
					words.add(token);
				}
			}
			return new QueryTerms(bigrams, words);
		}
	}

	private static List<String> tokenize(String text) {
		String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
		List<String> tokens = new ArrayList<>();
		Matcher matcher = TOKEN_PATTERN.matcher(normalized);
		while (matcher.find()) {
			tokens.add(matcher.group());
		}
		return tokens;
	}

	private static boolean isHangul(String token) {
		return HANGUL_PATTERN.matcher(token).matches();
	}

	private static String stripParticle(String token) {
		for (String particle : KOREAN_PARTICLES) {
			// 조사를 떼고도 2글자 이상 남을 때만 자른다 - "회의" 의 "의" 같은 경우를 보호한다.
			if (token.endsWith(particle) && token.length() - particle.length() >= 2) {
				return token.substring(0, token.length() - particle.length());
			}
		}
		return token;
	}

	private static void addBigrams(String token, Set<String> target) {
		for (int i = 0; i + 2 <= token.length(); i++) {
			target.add(token.substring(i, i + 2));
		}
	}
}
