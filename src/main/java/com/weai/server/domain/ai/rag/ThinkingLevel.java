package com.weai.server.domain.ai.rag;

import java.util.Locale;

/**
 * Replaces the raw numeric "RAG 검색 문서 수" (RAG document count) knob that used to be exposed
 * directly to users. A plain number doesn't map to anything a user can reason about; a named
 * depth level does. Governs how much project context {@link ProjectRagRetriever} pulls in, and
 * (for the single-agent Oracle chat) how much depth the model is asked to answer with.
 */
public enum ThinkingLevel {

	LOW(2, 0.72),
	DEFAULT(4, 0.65),
	HIGH(10, 0.55);

	private final int maxResults;
	private final double minScore;

	ThinkingLevel(int maxResults, double minScore) {
		this.maxResults = maxResults;
		this.minScore = minScore;
	}

	public int maxResults() {
		return maxResults;
	}

	public double minScore() {
		return minScore;
	}

	public static ThinkingLevel from(String rawLevel) {
		if (rawLevel == null || rawLevel.isBlank()) {
			return DEFAULT;
		}
		try {
			return ThinkingLevel.valueOf(rawLevel.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return DEFAULT;
		}
	}
}
