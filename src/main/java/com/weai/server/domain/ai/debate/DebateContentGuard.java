package com.weai.server.domain.ai.debate;

/**
 * Neutralizes attacker-controllable text before it enters a multi-agent debate prompt.
 *
 * Two distinct debate-specific risks exist on top of the generic RAG injection problem:
 *  1. {@link AiDebateService} embeds the editor's current code snippet inside a ``` markdown
 *     fence in every agent template. Code the developer is debating may originate from a
 *     teammate (or a compromised dependency comment), so a literal ``` sequence inside it can
 *     prematurely close that fence and make attacker text render as if it were outside the
 *     quoted code block.
 *  2. Debate completion is decided by a plain substring check for {@link #DEBATE_END_KEYWORD}
 *     in InspectorAi's raw output. If that exact keyword appears verbatim inside the code
 *     snippet or a RAG document, InspectorAi quoting it back (even innocently, e.g. "this
 *     comment looks suspicious: ...") would falsely end the debate early.
 */
final class DebateContentGuard {

	static final String DEBATE_END_KEYWORD = "[토론 종료]";

	private DebateContentGuard() {
	}

	static String sanitizeCodeSnippet(String snippet) {
		if (snippet == null) {
			return null;
		}
		return neutralizeEndKeyword(breakCodeFences(snippet));
	}

	static String sanitizeFreeText(String text) {
		if (text == null) {
			return null;
		}
		return neutralizeEndKeyword(text);
	}

	private static String breakCodeFences(String text) {
		return text.replace("```", "` ` `");
	}

	private static String neutralizeEndKeyword(String text) {
		return text.replace(DEBATE_END_KEYWORD, "[토론-종료-키워드]");
	}
}
