package com.weai.server.domain.ai.support;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Retries a single blocking AI model call with exponential backoff.
 *
 * langchain4j's Ollama client does not expose a stable exception hierarchy that distinguishes
 * transient failures (connection reset, timeout, HTTP 429/502/503) from permanent ones (bad
 * request, model not found), so this deliberately retries any {@link RuntimeException} rather than
 * trying to whitelist exception types that may not hold across versions. The attempt count is kept
 * small (callers typically pass 2) precisely because of that: it buys resilience against one-off
 * network blips without turning a real outage into an amplified retry storm against Ollama.
 *
 * Once every attempt has failed, a failure that means "the AI server could not be reached" (HTTP
 * 5xx such as the Cloudflare tunnel-down 1033/530 page, timeouts, connection refused, unknown host)
 * is translated into {@link ErrorCode#AI_SERVER_UNAVAILABLE} (503) so callers get a clear
 * "try again later" answer instead of a generic 500. Every other failure is rethrown unchanged.
 */
@Slf4j
public final class AiCallRetrier {

	private AiCallRetrier() {
	}

	public static <T> T withRetry(String label, int maxAttempts, long initialBackoffMs, Supplier<T> call) {
		RuntimeException lastError = null;
		long backoffMs = initialBackoffMs;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return call.get();
			} catch (RuntimeException exception) {
				lastError = exception;
				if (attempt == maxAttempts) {
					break;
				}
				log.warn(
					"{} call failed (attempt {}/{}), retrying in {}ms: {}",
					label, attempt, maxAttempts, backoffMs, exception.getMessage()
				);
				sleep(backoffMs);
				backoffMs *= 2;
			}
		}

		if (AiServerUnavailableDetector.isUnavailable(lastError)) {
			log.warn("{} call failed: AI server unreachable after {} attempt(s): {}", label, maxAttempts, lastError.getMessage());
			throw new ApiException(ErrorCode.AI_SERVER_UNAVAILABLE, ErrorCode.AI_SERVER_UNAVAILABLE.getMessage(), lastError);
		}
		throw lastError;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
		}
	}
}
