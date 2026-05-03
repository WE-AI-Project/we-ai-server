package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Project creation response")
public record ProjectCreateResponse(
	@Schema(description = "Project id", example = "1")
	Long projectId,

	@Schema(description = "Project name", example = "WE&AI Enterprise")
	String projectName,

	@Schema(description = "Generated project code", example = "PJ7X2K9A")
	String projectCode,

	@Schema(description = "Creator role", example = "LEADER")
	ProjectMemberRole role,

	@Schema(description = "Creator department", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "Project status", example = "ACTIVE")
	ProjectStatus status,

	@Schema(description = "Creation time", example = "2026-05-03T10:30:00")
	LocalDateTime createdAt
) {

	public static ProjectCreateResponse from(Project project, ProjectMember projectMember) {
		return new ProjectCreateResponse(
			project.getId(),
			project.getProjectName(),
			project.getProjectCode(),
			projectMember.getRole(),
			projectMember.getDepartment(),
			project.getStatus(),
			project.getCreatedAt()
		);
	}
}
