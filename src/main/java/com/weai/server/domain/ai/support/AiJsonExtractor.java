package com.weai.server.domain.ai.support;

/**
 * Extracts the JSON object an LLM was instructed to return, even when the model ignores the
 * "no markdown, no code fences" instruction and wraps the object in a ```json fence, adds a
 * leading/trailing sentence, or both. Every AI service in this codebase asks its model for a bare
 * JSON object and then calls {@code ObjectMapper.readTree} directly on the raw response, which
 * throws (and turns into a 500) the moment the model adds so much as a code fence around it.
 */
public final class AiJsonExtractor {

	private AiJsonExtractor() {
	}

	public static String extractJsonObject(String rawText) {
		if (rawText == null) {
			return null;
		}

		String candidate = stripCodeFence(rawText.trim());

		int start = candidate.indexOf('{');
		int end = candidate.lastIndexOf('}');
		if (start >= 0 && end > start) {
			return candidate.substring(start, end + 1);
		}
		return candidate;
	}

	private static String stripCodeFence(String text) {
		if (!text.startsWith("```")) {
			return text;
		}

		int firstNewline = text.indexOf('\n');
		if (firstNewline < 0) {
			return text;
		}
		String withoutOpeningFence = text.substring(firstNewline + 1);

		int closingFence = withoutOpeningFence.lastIndexOf("```");
		if (closingFence < 0) {
			return withoutOpeningFence.trim();
		}
		return withoutOpeningFence.substring(0, closingFence).trim();
	}
}
