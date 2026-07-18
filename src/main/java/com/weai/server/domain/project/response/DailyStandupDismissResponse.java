package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "오늘 다시 보지 않기 저장 응답")
public record DailyStandupDismissResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "사용자 ID", example = "3")
	Long userId,

	@Schema(description = "숨김 처리 날짜", example = "2026-05-25")
	LocalDate dismissDate,

	@Schema(description = "숨김 만료 시각", example = "2026-05-26T00:00:00")
	LocalDateTime dismissedUntil,

	@Schema(description = "모달 표시 여부", example = "false")
	boolean shouldShow
) {
}
