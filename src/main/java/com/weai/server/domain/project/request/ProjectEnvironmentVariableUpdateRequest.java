package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 환경변수 수정 요청")
public record ProjectEnvironmentVariableUpdateRequest(
	@Schema(description = "변경할 환경변수 값. 생략하면 기존 암호문을 유지합니다.", example = "example-secret")
	String value,

	@Schema(description = "변경할 Spring profile(local/dev/test/prod)", example = "prod")
	String profile,

	@Schema(description = "민감정보 여부", example = "true")
	Boolean secret,

	@Schema(description = "환경변수 설명", example = "운영 OpenAI API Key")
	String description,

	@Schema(description = "사용 여부", example = "true")
	Boolean enabled
) {
}
