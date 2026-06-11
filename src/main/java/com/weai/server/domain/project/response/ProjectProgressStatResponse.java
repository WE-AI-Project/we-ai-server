package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 진행률 통계 응답")
public record ProjectProgressStatResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "전체 일정 수", example = "12")
	long totalScheduleCount,

	@Schema(description = "TODO 일정 수", example = "3")
	long todoCount,

	@Schema(description = "진행 중 일정 수", example = "4")
	long inProgressCount,

	@Schema(description = "DONE 일정 수", example = "2")
	long doneCount,

	@Schema(description = "COMPLETED 일정 수", example = "1")
	long completedCount,

	@Schema(description = "HOLD 일정 수", example = "2")
	long holdCount,

	@Schema(description = "완료 처리된 일정 수", example = "3")
	long completedWorkCount,

	@Schema(description = "남은 일정 수", example = "9")
	long remainingScheduleCount,

	@Schema(description = "진행률", example = "25")
	int progressRate
) {
}
