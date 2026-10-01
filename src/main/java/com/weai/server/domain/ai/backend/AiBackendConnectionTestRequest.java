package com.weai.server.domain.ai.backend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Ad-hoc connection test for a candidate custom AI backend, before saving it")
public record AiBackendConnectionTestRequest(
	@NotBlank(message = "baseUrl is required.")
	String baseUrl,

	String dialect,

	String healthPath,

	@Schema(description = "API key to try. Leave blank to test without authentication.")
	String apiKey
) {
}
