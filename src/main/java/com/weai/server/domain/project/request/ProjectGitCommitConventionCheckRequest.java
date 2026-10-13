package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "커밋 컨벤션 검사 요청")
public record ProjectGitCommitConventionCheckRequest(
	@Schema(description = "검사할 커밋 제목 메시지", example = "feat: 채팅방 생성 API 구현")
	String message,

	@Schema(description = "선택 커밋 상세 설명", example = "일반 채팅방과 부서 채팅방 생성 기능을 추가합니다.")
	String description
) {
}
