package com.weai.server.domain.ai.backend;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Custom AI backend settings update request")
public record AiBackendSettingUpdateRequest(
	@Schema(description = "Whether this custom backend should be used instead of the default cluster", example = "true")
	Boolean enabled,

	@Schema(description = "Backend dialect: OLLAMA_NATIVE or OPENAI_COMPATIBLE", example = "OLLAMA_NATIVE")
	String dialect,

	@Schema(description = "Base URL of the custom backend", example = "https://my-gpu.example.com")
	String baseUrl,

	@Schema(description = "Model name to request on the custom backend", example = "llama3.1")
	String modelName,

	@Schema(description = "Health-check path used when testing the connection", example = "/api/tags")
	String healthPath,

	@Schema(description = "New API key. Null or blank keeps the currently stored key unchanged; use the dedicated delete endpoint to clear it.")
	String apiKey
) {
}
