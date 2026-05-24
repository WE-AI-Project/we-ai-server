package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Project schedule update request")
public record ProjectScheduleUpdateRequest(
	@Schema(description = "Schedule title", example = "프로젝트 일정 수정 API 구현")
	@Size(max = 100, message = "title must be 100 characters or fewer.")
	String title,

	@Schema(description = "Schedule description", example = "일정 수정 기능 개발")
	@Size(max = 1000, message = "description must be 1000 characters or fewer.")
	String description,

	@Schema(description = "Assignee user id", example = "7")
	Long assigneeId,

	@Schema(description = "Department", example = "BACKEND")
	String department,

	@Schema(description = "Start date", example = "2026-05-24")
	LocalDate startDate,

	@Schema(description = "End date", example = "2026-05-25")
	LocalDate endDate,

	@Schema(description = "Priority", example = "HIGH")
	String priority,

	@Schema(description = "Schedule status", example = "IN_PROGRESS")
	String status
) {
}
