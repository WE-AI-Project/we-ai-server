package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "프로젝트 환경변수 추가 또는 수정 응답")
public record ProjectEnvironmentVariableMutationResponse(
	@Schema(description = "환경변수 ID", example = "10")
	Long environmentVariableId,

	@Schema(description = "환경변수 키", example = "OPENAI_API_KEY")
	String key,

	@Schema(description = "Spring profile", example = "prod")
	String profile,

	@Schema(description = "민감정보 여부", example = "true")
	boolean secret,

	@Schema(description = "비밀값 마스킹. secret=false이면 null", example = "********", nullable = true)
	String maskedValue,

	@Schema(description = "환경변수 설명", example = "운영 OpenAI API Key", nullable = true)
	String description,

	@Schema(description = "사용 여부", example = "true")
	boolean enabled,

	@Schema(description = "생성 시각", example = "2026-09-15T13:20:00")
	LocalDateTime createdAt,

	@Schema(description = "수정 시각", example = "2026-09-15T13:30:00")
	LocalDateTime updatedAt
) {
}
