package com.weai.server.domain.user.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Current user recent activity list response")
public record UserRecentActivityListResponse(
	@Schema(description = "Applied limit", example = "10")
	int limit,

	@ArraySchema(schema = @Schema(implementation = ActivityResponse.class))
	List<ActivityResponse> activities
) {

	@Schema(description = "Current user recent activity item")
	public record ActivityResponse(
		@Schema(description = "Activity id", example = "schedule-10-updated")
		String activityId,

		@Schema(description = "Activity type", example = "SCHEDULE_UPDATED")
		String type,

		@Schema(description = "Related project id", example = "1")
		Long projectId,

		@Schema(description = "Related project name", example = "WE-AI Server")
		String projectName,

		@Schema(description = "Activity title", example = "Schedule updated")
		String title,

		@Schema(description = "Activity description", example = "API implementation schedule was updated.")
		String description,

		@Schema(description = "Activity target name", example = "API implementation")
		String targetName,

		@Schema(description = "Activity time", example = "2026-06-09T10:30:00")
		LocalDateTime createdAt
	) {
	}
}
