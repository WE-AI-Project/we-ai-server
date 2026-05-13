package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Project member list response")
public record ProjectMemberListResponse(
	@Schema(description = "Project id", example = "1")
	Long projectId,

	@ArraySchema(schema = @Schema(implementation = MemberResponse.class))
	List<MemberResponse> members
) {

	public static ProjectMemberListResponse from(Long projectId, List<ProjectMember> members) {
		return new ProjectMemberListResponse(projectId, members.stream().map(MemberResponse::from).toList());
	}

	@Schema(description = "Project member item")
	public record MemberResponse(
		@Schema(description = "User id", example = "1")
		Long userId,

		@Schema(description = "Display name", example = "Pyo Jaewon")
		String name,

		@Schema(description = "Email address", example = "test@example.com")
		String email,

		@Schema(description = "Profile image URL")
		String profileImageUrl,

		@Schema(description = "Project role", example = "LEADER")
		ProjectMemberRole role,

		@Schema(description = "Department", example = "BACKEND")
		ProjectDepartment department,

		@Schema(description = "Member status", example = "ACTIVE")
		ProjectMemberStatus status,

		@Schema(description = "Joined at", example = "2026-05-01T10:00:00")
		LocalDateTime joinedAt
	) {

		private static MemberResponse from(ProjectMember member) {
			return new MemberResponse(
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
}
