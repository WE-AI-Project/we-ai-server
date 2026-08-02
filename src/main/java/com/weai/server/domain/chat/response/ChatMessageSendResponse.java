package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatMessage;
import com.weai.server.domain.chat.domain.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 전송 응답")
public record ChatMessageSendResponse(
	@Schema(description = "메시지 ID", example = "102")
	Long messageId,

	@Schema(description = "채팅방 ID", example = "1")
	Long chatRoomId,

	@Schema(description = "발신자 ID", example = "3")
	Long senderId,

	@Schema(description = "발신자 이름", example = "김민혁")
	String senderName,

	@Schema(description = "메시지 타입", example = "TEXT")
	ChatMessageType messageType,

	@Schema(description = "메시지 내용", example = "오늘 프로젝트 일정 API 작업 완료했습니다.")
	String content,

	@Schema(description = "생성 시각", example = "2026-07-23T16:20:00")
	LocalDateTime createdAt
) {

	public static ChatMessageSendResponse from(ChatMessage message) {
		return new ChatMessageSendResponse(
			message.getId(),
			message.getChatRoom().getId(),
			message.getSender().getId(),
			message.getSender().getName(),
			message.getMessageType(),
			message.getContent(),
			message.getCreatedAt()
		);
	}
}
