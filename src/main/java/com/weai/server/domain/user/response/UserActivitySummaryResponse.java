package com.weai.server.domain.user.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current user activity summary response")
public record UserActivitySummaryResponse(
	@Schema(description = "Active project count", example = "3")
	long activeProjectCount,

	@Schema(description = "Project count where the user is a leader", example = "1")
	long leaderProjectCount,

	@Schema(description = "Schedule count assigned to the user in active projects", example = "12")
	long assignedScheduleCount,

	@Schema(description = "TODO schedule count assigned to the user", example = "4")
	long todoScheduleCount,

	@Schema(description = "IN_PROGRESS schedule count assigned to the user", example = "5")
	long inProgressScheduleCount,

	@Schema(description = "DONE or COMPLETED schedule count assigned to the user", example = "3")
	long completedScheduleCount,

	@Schema(description = "HOLD schedule count assigned to the user", example = "0")
	long holdScheduleCount,

	@Schema(description = "Completion rate for assigned schedules", example = "25")
	int completionRate
) {
}
