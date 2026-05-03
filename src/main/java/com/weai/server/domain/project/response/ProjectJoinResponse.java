package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "프로젝트 참여 응답")
public record ProjectJoinResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "프로젝트명", example = "WE&AI Enterprise")
	String projectName,

	@Schema(description = "프로젝트 참여 코드", example = "WEAI2025")
	String projectCode,

	@Schema(description = "참여 역할", example = "MEMBER")
	ProjectMemberRole role,

	@Schema(description = "참여 부서", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "참여 시각", example = "2026-05-03T11:00:00")
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
