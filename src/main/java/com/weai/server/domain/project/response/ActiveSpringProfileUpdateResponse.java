package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Active Spring Profile 변경 응답")
public record ActiveSpringProfileUpdateResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "변경 전 Active profile", example = "dev")
	String previousProfile,

	@Schema(description = "변경 후 Active profile", example = "prod")
	String activeProfile,

	@Schema(description = "변경한 사용자 ID", example = "3")
	Long changedBy,

	@Schema(description = "변경 시각", example = "2026-09-13T13:55:00")
	LocalDateTime changedAt,

	@Schema(description = "즉시 적용 여부", example = "false")
	boolean appliedImmediately,

	@Schema(description = "재시작 필요 여부", example = "true")
	boolean requiresRestart
) {
}
