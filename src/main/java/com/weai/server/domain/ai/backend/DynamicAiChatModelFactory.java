package com.weai.server.domain.ai.backend;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Builds a langchain4j {@link ChatModel} on demand for a resolved custom backend. Model instances
 * are cheap (just HTTP client config) so we build a fresh one per call instead of caching - custom
 * backends can be edited or disabled at any time and callers should always see the latest settings.
 */
@Component
public class DynamicAiChatModelFactory {

	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	public ChatModel build(ResolvedAiBackend backend, String fallbackModelName) {
		if (backend.dialect() == null) {
			throw new IllegalStateException("Resolved backend is missing a dialect.");
		}

		String modelName = StringUtils.hasText(backend.modelName()) ? backend.modelName() : fallbackModelName;
		String baseUrl = trimTrailingSlash(backend.baseUrl());

		return switch (backend.dialect()) {
			case OPENAI_COMPATIBLE -> OpenAiChatModel.builder()
				.baseUrl(baseUrl)
				.apiKey(StringUtils.hasText(backend.apiKey()) ? backend.apiKey() : "not-needed")
				.modelName(modelName)
				.timeout(TIMEOUT)
				.build();
			case OLLAMA_NATIVE -> OllamaChatModel.builder()
				.baseUrl(baseUrl)
				.modelName(modelName)
				.timeout(TIMEOUT)
				.customHeaders(authHeaders(backend.apiKey()))
				.build();
		};
	}

	private Map<String, String> authHeaders(String apiKey) {
		return StringUtils.hasText(apiKey) ? Map.of("Authorization", "Bearer " + apiKey) : Map.of();
	}

	private String trimTrailingSlash(String url) {
		return url == null ? null : url.replaceAll("/+$", "");
	}
}
