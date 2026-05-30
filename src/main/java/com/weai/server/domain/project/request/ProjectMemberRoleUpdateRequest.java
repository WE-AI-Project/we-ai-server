package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 멤버 역할 변경 요청")
public record ProjectMemberRoleUpdateRequest(
	@Schema(description = "변경할 프로젝트 멤버 역할", example = "MEMBER")
	String role
) {
}
