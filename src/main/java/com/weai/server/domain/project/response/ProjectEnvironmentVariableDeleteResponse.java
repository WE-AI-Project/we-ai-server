package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "프로젝트 환경변수 삭제 응답")
public record ProjectEnvironmentVariableDeleteResponse(
	@Schema(description = "삭제한 환경변수 ID", example = "10")
	Long environmentVariableId,

	@Schema(description = "삭제 시각", example = "2026-09-15T13:35:00")
	LocalDateTime deletedAt
) {
}
