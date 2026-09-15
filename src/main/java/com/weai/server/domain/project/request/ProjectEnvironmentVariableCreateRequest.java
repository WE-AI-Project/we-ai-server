package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 환경변수 추가 요청")
public record ProjectEnvironmentVariableCreateRequest(
	@Schema(description = "환경변수 키", example = "OPENAI_API_KEY", requiredMode = Schema.RequiredMode.REQUIRED)
	String key,

	@Schema(description = "환경변수 값", example = "example-secret", requiredMode = Schema.RequiredMode.REQUIRED)
	String value,

	@Schema(description = "Spring profile(local/dev/test/prod)", example = "dev", requiredMode = Schema.RequiredMode.REQUIRED)
	String profile,

	@Schema(description = "민감정보 여부", example = "true", defaultValue = "false")
	Boolean secret,

	@Schema(description = "환경변수 설명", example = "OpenAI API Key")
	String description,

	@Schema(description = "사용 여부", example = "true", defaultValue = "true")
	Boolean enabled
) {
}
