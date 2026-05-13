package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectSchedule;
import com.weai.server.domain.project.domain.ProjectSchedulePriority;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Project schedule list response")
public record ProjectScheduleListResponse(
	@Schema(description = "Project id", example = "1")
	Long projectId,

	@ArraySchema(schema = @Schema(implementation = ScheduleResponse.class))
	List<ScheduleResponse> schedules
) {

	public static ProjectScheduleListResponse from(Long projectId, List<ProjectSchedule> schedules) {
		return new ProjectScheduleListResponse(projectId, schedules.stream().map(ScheduleResponse::from).toList());
	}

	@Schema(description = "Project schedule item")
	public record ScheduleResponse(
		@Schema(description = "Schedule id", example = "1")
		Long scheduleId,

		@Schema(description = "Title", example = "Implement dashboard API")
		String title,

		@Schema(description = "Description", example = "Build dashboard summary endpoint and DTOs.")
		String description,

		@Schema(description = "Assignee id", example = "1")
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

		public static ScheduleResponse from(ProjectSchedule schedule) {
			return new ScheduleResponse(
				schedule.getId(),
				schedule.getTitle(),
				schedule.getDescription(),
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
}
