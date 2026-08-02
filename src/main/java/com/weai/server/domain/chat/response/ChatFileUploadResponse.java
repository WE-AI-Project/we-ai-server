package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatMessage;
import com.weai.server.domain.chat.domain.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅 파일 업로드 응답")
public record ChatFileUploadResponse(
	@Schema(description = "메시지 ID", example = "103")
	Long messageId,

	@Schema(description = "채팅방 ID", example = "1")
	Long chatRoomId,

	@Schema(description = "발신자 ID", example = "3")
	Long senderId,

	@Schema(description = "발신자 이름", example = "김민혁")
	String senderName,

	@Schema(description = "메시지 타입", example = "IMAGE")
	ChatMessageType messageType,

	@Schema(description = "파일과 함께 보낸 메시지", example = "화면 캡처 공유합니다.")
	String content,

	@Schema(description = "파일 URL", example = "/uploads/chat/1/1/550e8400-e29b-41d4-a716-446655440000.png")
	String fileUrl,

	@Schema(description = "원본 파일명", example = "sample.png")
	String originalFileName,

	@Schema(description = "파일 크기", example = "245121")
	Long fileSize,

	@Schema(description = "파일 Content-Type", example = "image/png")
	String fileContentType,

	@Schema(description = "생성 시각", example = "2026-07-23T16:30:00")
	LocalDateTime createdAt
) {

	public static ChatFileUploadResponse from(ChatMessage message) {
		return new ChatFileUploadResponse(
			message.getId(),
			message.getChatRoom().getId(),
			message.getSender().getId(),
			message.getSender().getName(),
			message.getMessageType(),
			message.getContent(),
			message.getFileUrl(),
			message.getOriginalFileName(),
			message.getFileSize(),
			message.getFileContentType(),
			message.getCreatedAt()
		);
	}
}
