package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project schedule status update request")
public record ProjectScheduleStatusUpdateRequest(
	@Schema(description = "Schedule status to update", example = "DONE", requiredMode = Schema.RequiredMode.REQUIRED)
	String status
) {
}
