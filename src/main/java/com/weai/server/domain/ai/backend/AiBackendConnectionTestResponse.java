package com.weai.server.domain.ai.backend;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of an ad-hoc custom AI backend connection test")
public record AiBackendConnectionTestResponse(
	boolean ok,
	Long latencyMs,
	Integer modelsFound,
	String reason
) {
	public static AiBackendConnectionTestResponse success(long latencyMs, Integer modelsFound) {
		return new AiBackendConnectionTestResponse(true, latencyMs, modelsFound, null);
	}

	public static AiBackendConnectionTestResponse failure(String reason) {
		return new AiBackendConnectionTestResponse(false, null, null, reason);
	}
}
