package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 멤버 추방 응답")
public record ProjectMemberKickResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "프로젝트 멤버 ID", example = "12")
	Long projectMemberId,

	@Schema(description = "추방된 사용자 ID", example = "5")
	Long userId,

	@Schema(description = "추방된 사용자 이름", example = "홍길동")
	String name,

	@Schema(description = "변경된 프로젝트 멤버 상태", example = "KICKED")
	ProjectMemberStatus status
) {

	public static ProjectMemberKickResponse from(ProjectMember member) {
		return new ProjectMemberKickResponse(
			member.getProject().getId(),
			member.getId(),
			member.getUser().getId(),
			member.getUser().getName(),
			member.getStatus()
		);
	}
}
