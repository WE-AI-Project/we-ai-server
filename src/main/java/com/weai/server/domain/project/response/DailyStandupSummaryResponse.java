package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Daily standup summary response")
public record DailyStandupSummaryResponse(
	@Schema(description = "Project ID", example = "1")
	Long projectId,

	@Schema(description = "Project name", example = "Synaipse Project")
	String projectName,

	@Schema(description = "Summary date", example = "2026-07-23")
	LocalDate today,

	@Schema(description = "Whether to show the daily standup modal", example = "true")
	boolean shouldShow,

	@Schema(description = "Previous project access time for the current user", example = "2026-07-22T18:30:00")
	LocalDateTime lastAccessedAt,

	@Schema(description = "Current summary query time", example = "2026-07-23T17:40:00")
	LocalDateTime currentAccessedAt,

	@Schema(description = "Summary start time", example = "2026-07-22T18:30:00")
	LocalDateTime summaryFrom,

	@Schema(description = "Summary end time", example = "2026-07-23T17:40:00")
	LocalDateTime summaryTo,

	@Schema(description = "Daily standup summary counts")
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

	@Schema(description = "Daily standup summary counts")
	public record Summary(
		@Schema(description = "Total changed item count", example = "8")
		long totalChangedCount,

		@Schema(description = "Completed schedule count", example = "3")
		long completedCount,

		@Schema(description = "In-progress schedule count", example = "2")
		long inProgressCount,

		@Schema(description = "Todo schedule count", example = "2")
		long todoCount,

		@Schema(description = "Blocked schedule count", example = "1")
		long blockerCount
	) {
	}
}
