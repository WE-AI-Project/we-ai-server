package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 초대 코드 재발급 응답")
public record ProjectInviteCodeReissueResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "이전 초대 코드", example = "WEAI2025")
	String oldInviteCode,

	@Schema(description = "새 초대 코드", example = "SYNA8K21")
	String newInviteCode
) {
}
