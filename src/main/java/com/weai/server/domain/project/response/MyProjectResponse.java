package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "My project summary")
public record MyProjectResponse(
	@Schema(description = "Project id", example = "1")
	Long projectId,

	@Schema(description = "Project name", example = "WE&AI Backend Server")
	String projectName,

	@Schema(description = "Project description", example = "AI-powered backend for collaboration")
	String description,

	@Schema(description = "Project invite code", example = "WEAI2025")
	String projectCode,

	@Schema(description = "My role", example = "LEADER")
	ProjectMemberRole role,

	@Schema(description = "My department", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "Project status", example = "ACTIVE")
	ProjectStatus status,

	@ArraySchema(schema = @Schema(example = "Spring Boot"))
	List<String> techStacks,

	@Schema(description = "Project start date", example = "2026-05-03")
	LocalDate startDate,

	@Schema(description = "Project target date", example = "2026-06-30")
	LocalDate targetDate,

	@Schema(description = "Current active member count", example = "6")
	long memberCount,

	@Schema(description = "Creation time", example = "2026-05-03T10:00:00")
	LocalDateTime createdAt
) {

	public static MyProjectResponse from(ProjectMember projectMember, List<String> techStacks, long memberCount) {
		Project project = projectMember.getProject();
		return new MyProjectResponse(
			project.getId(),
			project.getProjectName(),
			project.getDescription(),
			project.getProjectCode(),
			projectMember.getRole(),
			projectMember.getDepartment(),
			project.getStatus(),
			techStacks,
			project.getStartDate(),
			project.getTargetDate(),
			memberCount,
			project.getCreatedAt()
		);
	}
}
