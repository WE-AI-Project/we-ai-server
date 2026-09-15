package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "프로젝트 환경변수 목록 조회 응답")
public record ProjectEnvironmentVariableListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "조회 profile 필터", example = "dev", nullable = true)
	String profile,

	@Schema(description = "조회된 환경변수 개수", example = "2")
	long totalCount,

	@ArraySchema(schema = @Schema(implementation = EnvironmentVariableResponse.class))
	List<EnvironmentVariableResponse> variables
) {

	@Schema(description = "환경변수 정보")
	public record EnvironmentVariableResponse(
		@Schema(description = "환경변수 ID", example = "10")
		Long environmentVariableId,

		@Schema(description = "환경변수 키", example = "OPENAI_API_KEY")
		String key,

		@Schema(description = "비밀값이 아닌 경우에만 반환되는 원본 값", example = "http://localhost:8080", nullable = true)
		String value,

		@Schema(description = "비밀값 마스킹", example = "********", nullable = true)
		String maskedValue,

		@Schema(description = "Spring profile", example = "dev")
		String profile,

		@Schema(description = "민감정보 여부", example = "true")
		boolean secret,

		@Schema(description = "환경변수 설명", example = "OpenAI API Key", nullable = true)
		String description,

		@Schema(description = "사용 여부", example = "true")
		boolean enabled,

		@Schema(description = "생성 시각", example = "2026-09-15T13:20:00")
		LocalDateTime createdAt,

		@Schema(description = "수정 시각", example = "2026-09-15T13:30:00")
		LocalDateTime updatedAt
	) {
	}
}
