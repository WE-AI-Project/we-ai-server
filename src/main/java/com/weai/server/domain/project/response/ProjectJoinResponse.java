package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Project join response")
public record ProjectJoinResponse(
	@Schema(description = "Project id", example = "1")
	Long projectId,

	@Schema(description = "Project name", example = "WE&AI Enterprise")
	String projectName,

	@Schema(description = "Project invite code", example = "WEAI2025")
	String projectCode,

	@Schema(description = "Joined role", example = "MEMBER")
	ProjectMemberRole role,

	@Schema(description = "Joined department", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "Join time", example = "2026-05-03T11:00:00")
	LocalDateTime joinedAt
) {

	public static ProjectJoinResponse from(ProjectMember projectMember) {
		return new ProjectJoinResponse(
			projectMember.getProject().getId(),
			projectMember.getProject().getProjectName(),
			projectMember.getProject().getProjectCode(),
			projectMember.getRole(),
			projectMember.getDepartment(),
			projectMember.getJoinedAt()
		);
	}
}
