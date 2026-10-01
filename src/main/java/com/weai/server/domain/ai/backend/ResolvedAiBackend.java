package com.weai.server.domain.ai.backend;

/**
 * The effective AI backend to use for a single request, after applying the precedence rule
 * personal setting (if enabled) &gt; project-shared setting (if enabled) &gt; default cluster.
 * {@code source == DEFAULT} means "use the fixed Ollama cluster beans as-is" - baseUrl/apiKey are
 * unset in that case since the caller already has beans wired for it.
 */
public record ResolvedAiBackend(
	Source source,
	AiBackendDialect dialect,
	String baseUrl,
	String modelName,
	String apiKey
) {
	public enum Source {
		DEFAULT, PERSONAL, PROJECT
	}

	public static final ResolvedAiBackend DEFAULT = new ResolvedAiBackend(Source.DEFAULT, null, null, null, null);

	public boolean isDefault() {
		return source == Source.DEFAULT;
	}
}
