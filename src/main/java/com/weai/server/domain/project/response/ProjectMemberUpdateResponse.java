package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 멤버 정보 변경 응답")
public record ProjectMemberUpdateResponse(
	@Schema(description = "프로젝트 멤버 ID", example = "10")
	Long projectMemberId,

	@Schema(description = "사용자 ID", example = "3")
	Long userId,

	@Schema(description = "사용자 이름", example = "김민혁")
	String name,

	@Schema(description = "프로젝트 역할", example = "MEMBER")
	ProjectMemberRole role,

	@Schema(description = "프로젝트 부서", example = "FRONTEND")
	ProjectDepartment department,

	@Schema(description = "프로젝트 멤버 상태", example = "ACTIVE")
	ProjectMemberStatus status
) {

	public static ProjectMemberUpdateResponse from(ProjectMember member) {
		return new ProjectMemberUpdateResponse(
			member.getId(),
			member.getUser().getId(),
			member.getUser().getName(),
			member.getRole(),
			member.getDepartment(),
			member.getStatus()
		);
	}
}
