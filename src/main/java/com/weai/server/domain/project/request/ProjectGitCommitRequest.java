package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Git 커밋 생성 요청")
public record ProjectGitCommitRequest(
	@Schema(description = "커밋 제목 메시지", example = "feat: 변경 파일 목록 조회 API 구현")
	String message,

	@Schema(description = "커밋 상세 설명", example = "Changes 화면에서 파일 목록과 diff를 확인할 수 있도록 API를 추가했습니다.")
	String description
) {
}
