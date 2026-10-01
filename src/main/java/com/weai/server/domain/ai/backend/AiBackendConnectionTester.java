package com.weai.server.domain.ai.backend;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Probes a candidate custom AI backend from the server side (not the browser), so this works
 * regardless of the target's CORS policy and never exposes the API key to the client.
 *
 * {@link PrivateNetworkGuard} is applied first so this can't be turned into an SSRF probe against
 * Docker-internal services or this host's private network. Redirects are not followed (reactor-netty's
 * default), so a 3xx response can't be used to bounce past that check.
 */
@Component
@RequiredArgsConstructor
public class AiBackendConnectionTester {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private static final String DEFAULT_HEALTH_PATH = "/api/tags";

	private final PrivateNetworkGuard privateNetworkGuard;
	private final WebClient webClient = WebClient.builder().build();

	public AiBackendConnectionTestResponse test(String baseUrl, String healthPath, String apiKey) {
		if (!StringUtils.hasText(baseUrl)) {
			return AiBackendConnectionTestResponse.failure("baseUrl is required.");
		}

		String url = trimTrailingSlash(baseUrl) + (StringUtils.hasText(healthPath) ? healthPath : DEFAULT_HEALTH_PATH);

		try {
			privateNetworkGuard.assertPubliclyRoutable(url);
		} catch (IllegalArgumentException invalidTarget) {
			return AiBackendConnectionTestResponse.failure("허용되지 않는 주소입니다. 공개적으로 접근 가능한 호스트를 입력해 주세요.");
		}

		long started = System.currentTimeMillis();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> body = webClient.get()
				.uri(url)
				.headers(headers -> {
					if (StringUtils.hasText(apiKey)) {
						headers.set("Authorization", "Bearer " + apiKey);
					}
				})
				.retrieve()
				.bodyToMono(Map.class)
				.timeout(TIMEOUT)
				.block();

			long latencyMs = System.currentTimeMillis() - started;
			Integer modelsFound = null;
			if (body != null && body.get("models") instanceof List<?> models) {
				modelsFound = models.size();
			}
			return AiBackendConnectionTestResponse.success(latencyMs, modelsFound);
		} catch (WebClientResponseException httpError) {
			return AiBackendConnectionTestResponse.failure(
				"서버가 " + httpError.getStatusCode().value() + " " + httpError.getStatusText() + "로 응답했습니다."
			);
		} catch (Exception exception) {
			String cause = exception.getCause() != null ? exception.getCause().getClass().getSimpleName() : exception.getClass().getSimpleName();
			if (cause.contains("Timeout")) {
				return AiBackendConnectionTestResponse.failure("5초 내 응답이 없습니다. 주소나 포트, 터널 상태를 확인해 주세요.");
			}
			return AiBackendConnectionTestResponse.failure("연결할 수 없습니다. 주소, 포트, 또는 방화벽/터널 상태를 확인하세요.");
		}
	}

	private String trimTrailingSlash(String url) {
		return url.replaceAll("/+$", "");
	}
}
