package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectSchedule;
import com.weai.server.domain.project.domain.ProjectSchedulePriority;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Project schedule create response")
public record ProjectScheduleCreateResponse(
	@Schema(description = "Schedule id", example = "1")
	Long scheduleId,

	@Schema(description = "Project id", example = "1")
	Long projectId,

	@Schema(description = "Title", example = "Implement dashboard API")
	String title,

	@Schema(description = "Assignee id", example = "2")
	Long assigneeId,

	@Schema(description = "Assignee name", example = "Pyo Jaewon")
	String assigneeName,

	@Schema(description = "Department", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "Start date", example = "2026-05-04")
	LocalDate startDate,

	@Schema(description = "End date", example = "2026-05-05")
	LocalDate endDate,

	@Schema(description = "Priority", example = "HIGH")
	ProjectSchedulePriority priority,

	@Schema(description = "Status", example = "TODO")
	ProjectScheduleStatus status,

	@Schema(description = "Created at", example = "2026-05-03T10:00:00")
	LocalDateTime createdAt
) {

	public static ProjectScheduleCreateResponse from(ProjectSchedule schedule) {
		return new ProjectScheduleCreateResponse(
			schedule.getId(),
			schedule.getProject().getId(),
			schedule.getTitle(),
			schedule.getAssignee().getId(),
			schedule.getAssignee().getName(),
			schedule.getDepartment(),
			schedule.getStartDate(),
			schedule.getEndDate(),
			schedule.getPriority(),
			schedule.getStatus(),
			schedule.getCreatedAt()
		);
	}
}
