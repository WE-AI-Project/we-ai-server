package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 나가기 응답")
public record ProjectLeaveResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "프로젝트 멤버 ID", example = "10")
	Long projectMemberId,

	@Schema(description = "사용자 ID", example = "3")
	Long userId,

	@Schema(description = "변경된 프로젝트 멤버 상태", example = "LEFT")
	ProjectMemberStatus status
) {

	public static ProjectLeaveResponse from(ProjectMember member) {
		return new ProjectLeaveResponse(
			member.getProject().getId(),
			member.getId(),
			member.getUser().getId(),
			member.getStatus()
		);
	}
}
