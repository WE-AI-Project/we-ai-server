package com.weai.server.domain.ai.support;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Bounds how often one caller can trigger an (expensive, shared-infrastructure) AI call.
 *
 * Nothing in the AI controllers previously limited request frequency: an authenticated user could
 * call {@code /debate/custom} with up to 20 rounds x 4 agents back-to-back as many times as they
 * wanted, with no per-user or per-endpoint ceiling. This is a plain in-memory fixed-window limiter
 * (no new dependency, no shared state across instances) - adequate for a single-instance
 * deployment; a multi-instance deployment would need a shared store (e.g. Redis) instead.
 */
@Component
public class AiRateLimiterService {

	private static final class Window {
		private long windowStartMs;
		private int count;
	}

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	/**
	 * @param key               identifies the caller+endpoint being limited, e.g. {@code "chat:42"}.
	 * @param maxCallsPerWindow how many calls {@code key} may make per {@code windowMs}.
	 * @param windowMs          the fixed window size in milliseconds.
	 * @throws ApiException with {@link ErrorCode#TOO_MANY_REQUESTS} once the limit is exceeded.
	 */
	public void checkAndConsume(String key, int maxCallsPerWindow, long windowMs) {
		Window window = windows.computeIfAbsent(key, ignored -> new Window());
		synchronized (window) {
			long now = System.currentTimeMillis();
			if (now - window.windowStartMs >= windowMs) {
				window.windowStartMs = now;
				window.count = 0;
			}
			if (window.count >= maxCallsPerWindow) {
				throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
			}
			window.count++;
		}
	}
}
