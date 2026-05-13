package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectSchedule;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Project dashboard response")
public record ProjectDashboardResponse(
	@Schema(description = "Project id", example = "1")
	Long projectId,

	@Schema(description = "Project name", example = "WE&AI Enterprise")
	String projectName,

	@Schema(description = "Project join code", example = "WEAI2025")
	String projectCode,

	@Schema(description = "Project status", example = "ACTIVE")
	ProjectStatus status,

	@Schema(description = "Project start date", example = "2026-05-01")
	LocalDate startDate,

	@Schema(description = "Project target date", example = "2026-06-30")
	LocalDate targetDate,

	@Schema(description = "Active member count", example = "6")
	long memberCount,

	@Schema(description = "Total schedule count", example = "12")
	long scheduleCount,

	@Schema(description = "Completed schedule count", example = "5")
	long completedScheduleCount,

	@Schema(description = "Overall progress rate", example = "42")
	int progressRate,

	@ArraySchema(schema = @Schema(implementation = DepartmentProgressResponse.class))
	List<DepartmentProgressResponse> departmentProgress,

	@ArraySchema(schema = @Schema(implementation = RecentScheduleResponse.class))
	List<RecentScheduleResponse> recentSchedules
) {

	public static ProjectDashboardResponse of(
		Project project,
		long memberCount,
		long scheduleCount,
		long completedScheduleCount,
		int progressRate,
		List<DepartmentProgressResponse> departmentProgress,
		List<RecentScheduleResponse> recentSchedules
	) {
		return new ProjectDashboardResponse(
			project.getId(),
			project.getProjectName(),
			project.getProjectCode(),
			project.getStatus(),
			project.getStartDate(),
			project.getTargetDate(),
			memberCount,
			scheduleCount,
			completedScheduleCount,
			progressRate,
			departmentProgress,
			recentSchedules
		);
	}

	@Schema(description = "Department progress item")
	public record DepartmentProgressResponse(
		@Schema(description = "Department", example = "BACKEND")
		ProjectDepartment department,

		@Schema(description = "Total schedule count", example = "5")
		long totalCount,

		@Schema(description = "Completed schedule count", example = "3")
		long completedCount,

		@Schema(description = "Progress rate", example = "60")
		int progressRate
	) {

		public static DepartmentProgressResponse of(ProjectDepartment department, long totalCount, long completedCount) {
			int progressRate = totalCount == 0 ? 0 : (int) ((completedCount * 100) / totalCount);
			return new DepartmentProgressResponse(department, totalCount, completedCount, progressRate);
		}
	}

	@Schema(description = "Recent schedule item")
	public record RecentScheduleResponse(
		@Schema(description = "Schedule id", example = "1")
		Long scheduleId,

		@Schema(description = "Title", example = "Implement dashboard API")
		String title,

		@Schema(description = "Department", example = "BACKEND")
		ProjectDepartment department,

		@Schema(description = "Status", example = "DONE")
		ProjectScheduleStatus status,

		@Schema(description = "End date", example = "2026-05-05")
		LocalDate endDate
	) {

		public static RecentScheduleResponse from(ProjectSchedule schedule) {
			return new RecentScheduleResponse(
				schedule.getId(),
				schedule.getTitle(),
				schedule.getDepartment(),
				schedule.getStatus(),
				schedule.getEndDate()
			);
		}
	}
}
