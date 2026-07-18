package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "데일리 스탠드업 요약 조회 응답")
public record DailyStandupSummaryResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "프로젝트 이름", example = "Synaipse Project")
	String projectName,

	@Schema(description = "조회 기준 날짜", example = "2026-05-25")
	LocalDate today,

	@Schema(description = "모달 표시 여부", example = "true")
	boolean shouldShow,

	@Schema(description = "요약 카운트")
	Summary summary,

	@ArraySchema(schema = @Schema(implementation = DailyStandupItemResponse.class))
	List<DailyStandupItemResponse> completedItems,

	@ArraySchema(schema = @Schema(implementation = DailyStandupItemResponse.class))
	List<DailyStandupItemResponse> inProgressItems,

	@ArraySchema(schema = @Schema(implementation = DailyStandupItemResponse.class))
	List<DailyStandupItemResponse> blockerItems,

	@ArraySchema(schema = @Schema(implementation = DailyStandupActivityResponse.class))
	List<DailyStandupActivityResponse> recentActivities
) {

	@Schema(description = "데일리 스탠드업 요약 카운트")
	public record Summary(
		@Schema(description = "오늘 변경된 전체 항목 수", example = "8")
		long totalChangedCount,

		@Schema(description = "완료 일정 수", example = "3")
		long completedCount,

		@Schema(description = "진행 중 일정 수", example = "2")
		long inProgressCount,

		@Schema(description = "할 일 일정 수", example = "2")
		long todoCount,

		@Schema(description = "블로커 일정 수", example = "1")
		long blockerCount
	) {
	}
}
