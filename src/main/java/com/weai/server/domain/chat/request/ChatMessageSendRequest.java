package com.weai.server.domain.chat.request;

import com.weai.server.domain.chat.domain.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 메시지 전송 요청")
public record ChatMessageSendRequest(
	@Schema(description = "메시지 내용", example = "오늘 프로젝트 일정 API 작업 완료했습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
	String content,

	@Schema(description = "메시지 타입. 생략하면 TEXT로 처리됩니다.", example = "TEXT")
	ChatMessageType messageType
) {
}
