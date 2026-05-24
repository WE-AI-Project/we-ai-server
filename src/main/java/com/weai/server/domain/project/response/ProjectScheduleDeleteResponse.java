package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project schedule delete response")
public record ProjectScheduleDeleteResponse(
	@Schema(description = "Deleted schedule id", example = "10")
	Long scheduleId
) {

	public static ProjectScheduleDeleteResponse from(Long scheduleId) {
		return new ProjectScheduleDeleteResponse(scheduleId);
	}
}
