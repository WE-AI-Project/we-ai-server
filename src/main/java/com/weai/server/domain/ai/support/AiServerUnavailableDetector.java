package com.weai.server.domain.ai.support;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.LangChain4jException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.exception.UnresolvedModelServerException;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Decides whether a failed langchain4j model call means "the AI server is unreachable right now".
 *
 * langchain4j's ExceptionMapper turns HTTP 5xx into {@link InternalServerException} (this is what a
 * Cloudflare "error code: 1033" tunnel-down page becomes), HTTP 408 into {@link TimeoutException}
 * and an unresolvable host into {@link UnresolvedModelServerException}; transport-level failures
 * (connection refused, HTTP client timeouts) arrive as a plain {@link LangChain4jException} wrapping
 * the underlying {@link IOException}. Only failures that originate from langchain4j count - an
 * unrelated exception that happens to wrap an IOException is never reclassified.
 */
public final class AiServerUnavailableDetector {

	private AiServerUnavailableDetector() {
	}

	public static boolean isUnavailable(Throwable failure) {
		if (failure == null) {
			return false;
		}

		boolean fromLangChain4j = false;
		boolean ioFailure = false;
		Map<Throwable, Boolean> visited = new IdentityHashMap<>();
		for (Throwable current = failure; current != null && visited.put(current, Boolean.TRUE) == null; current = current.getCause()) {
			if (current instanceof InternalServerException
				|| current instanceof TimeoutException
				|| current instanceof UnresolvedModelServerException) {
				return true;
			}
			if (current instanceof HttpException httpException && httpException.statusCode() >= 500) {
				return true;
			}
			if (current instanceof LangChain4jException) {
				fromLangChain4j = true;
			}
			if (current instanceof IOException || current instanceof java.util.concurrent.TimeoutException) {
				ioFailure = true;
			}
		}
		return fromLangChain4j && ioFailure;
	}
}
