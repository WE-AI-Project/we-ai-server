package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectSchedule;
import com.weai.server.domain.project.domain.ProjectSchedulePriority;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Project schedule detail response")
public record ProjectScheduleDetailResponse(
	@Schema(description = "Schedule id", example = "10")
	Long scheduleId,

	@Schema(description = "Project id", example = "1")
	Long projectId,

	@Schema(description = "Schedule title", example = "Implement project schedule detail API")
	String title,

	@Schema(description = "Schedule description", example = "Develop schedule detail query API")
	String description,

	@Schema(description = "Assignee user id", example = "3")
	Long assigneeId,

	@Schema(description = "Assignee name", example = "Pyo Jaewon")
	String assigneeName,

	@Schema(description = "Department", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "Start date", example = "2026-05-23")
	LocalDate startDate,

	@Schema(description = "End date", example = "2026-05-23")
	LocalDate endDate,

	@Schema(description = "Priority", example = "HIGH")
	ProjectSchedulePriority priority,

	@Schema(description = "Schedule status", example = "TODO")
	ProjectScheduleStatus status,

	@Schema(description = "Created at", example = "2026-05-23T10:00:00")
	LocalDateTime createdAt,

	@Schema(description = "Updated at", example = "2026-05-23T10:00:00")
	LocalDateTime updatedAt
) {

	public static ProjectScheduleDetailResponse from(ProjectSchedule schedule) {
		return new ProjectScheduleDetailResponse(
			schedule.getId(),
			schedule.getProject().getId(),
			schedule.getTitle(),
			schedule.getDescription(),
			schedule.getAssignee().getId(),
			schedule.getAssignee().getName(),
			schedule.getDepartment(),
			schedule.getStartDate(),
			schedule.getEndDate(),
			schedule.getPriority(),
			schedule.getStatus(),
			schedule.getCreatedAt(),
			schedule.getUpdatedAt()
		);
	}
}
