package com.weai.server.domain.ai.backend;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Custom AI backend settings")
public record AiBackendSettingResponse(
	boolean enabled,
	String dialect,
	String baseUrl,
	String modelName,
	String healthPath,
	@Schema(description = "true if an API key is currently stored (the key itself is never returned)")
	boolean hasApiKey,
	LocalDateTime updatedAt
) {
	public static AiBackendSettingResponse from(UserAiBackendSetting setting) {
		return new AiBackendSettingResponse(
			setting.isEnabled(),
			setting.getDialect().name(),
			setting.getBaseUrl(),
			setting.getModelName(),
			setting.getHealthPath(),
			setting.getEncryptedApiKey() != null,
			setting.getUpdatedAt()
		);
	}

	public static AiBackendSettingResponse from(ProjectAiBackendSetting setting) {
		return new AiBackendSettingResponse(
			setting.isEnabled(),
			setting.getDialect().name(),
			setting.getBaseUrl(),
			setting.getModelName(),
			setting.getHealthPath(),
			setting.getEncryptedApiKey() != null,
			setting.getUpdatedAt()
		);
	}
}
