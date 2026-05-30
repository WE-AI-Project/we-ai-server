package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "프로젝트 멤버 상세 조회 응답")
public record ProjectMemberDetailResponse(
	@Schema(description = "프로젝트 멤버 ID", example = "10")
	Long projectMemberId,

	@Schema(description = "사용자 ID", example = "3")
	Long userId,

	@Schema(description = "사용자 이름", example = "김민혁")
	String name,

	@Schema(description = "사용자 이메일", example = "minhyuk@example.com")
	String email,

	@Schema(description = "프로필 이미지 URL")
	String profileImageUrl,

	@Schema(description = "프로젝트 역할", example = "MEMBER")
	ProjectMemberRole role,

	@Schema(description = "프로젝트 부서", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "프로젝트 멤버 상태", example = "ACTIVE")
	ProjectMemberStatus status,

	@Schema(description = "프로젝트 참여 시각", example = "2026-05-01T10:00:00")
	LocalDateTime joinedAt
) {

	public static ProjectMemberDetailResponse from(ProjectMember member) {
		return new ProjectMemberDetailResponse(
			member.getId(),
			member.getUser().getId(),
			member.getUser().getName(),
			member.getUser().getEmail(),
			null,
			member.getRole(),
			member.getDepartment(),
			member.getStatus(),
			member.getJoinedAt()
		);
	}
}
