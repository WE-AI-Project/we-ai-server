package com.weai.server.domain.chat.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회의 모드 시작 요청")
public record MeetingStartRequest(
	@Schema(description = "회의 제목", example = "백엔드 API 회의")
	String title,

	@Schema(description = "회의 설명", example = "채팅 문서 및 회의록 기능 논의")
	String description,

	@Schema(description = "연결할 채팅방 ID", example = "1")
	Long chatRoomId
) {
}
