package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatMessage;
import com.weai.server.domain.chat.domain.ChatMessageType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "채팅 메시지 목록 조회 응답")
public record ChatMessageListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "채팅방 ID", example = "1")
	Long chatRoomId,

	@Schema(description = "조회 크기", example = "30")
	int size,

	@Schema(description = "다음 페이지 존재 여부", example = "true")
	boolean hasNext,

	@Schema(description = "다음 조회에 사용할 커서 메시지 ID", example = "70")
	Long nextCursor,

	@ArraySchema(schema = @Schema(implementation = ChatMessageResponse.class))
	List<ChatMessageResponse> messages
) {

	public static ChatMessageListResponse from(
		Long projectId,
		Long chatRoomId,
		int size,
		boolean hasNext,
		Long nextCursor,
		List<ChatMessage> messages,
		Long currentUserId
	) {
		return new ChatMessageListResponse(
			projectId,
			chatRoomId,
			size,
			hasNext,
			nextCursor,
			messages.stream()
				.map(message -> ChatMessageResponse.from(message, currentUserId))
				.toList()
		);
	}

	@Schema(description = "채팅 메시지 목록 아이템")
	public record ChatMessageResponse(
		@Schema(description = "메시지 ID", example = "101")
		Long messageId,

		@Schema(description = "채팅방 ID", example = "1")
		Long chatRoomId,

		@Schema(description = "발신자 ID", example = "3")
		Long senderId,

		@Schema(description = "발신자 이름", example = "김민혁")
		String senderName,

		@Schema(description = "발신자 프로필 이미지 URL", example = "https://example.com/profile.png")
		String senderProfileImageUrl,

		@Schema(description = "메시지 타입", example = "TEXT")
		ChatMessageType messageType,

		@Schema(description = "메시지 내용", example = "오늘 채팅 API 작업 진행하겠습니다.")
		String content,

		@Schema(description = "파일 URL", example = "/uploads/chat/1/1/file.png")
		String fileUrl,

		@Schema(description = "원본 파일명", example = "sample.png")
		String originalFileName,

		@Schema(description = "파일 크기", example = "245121")
		Long fileSize,

		@Schema(description = "파일 Content-Type", example = "image/png")
		String fileContentType,

		@Schema(description = "내가 보낸 메시지 여부", example = "true")
		boolean isMine,

		@Schema(description = "생성 시각", example = "2026-07-23T16:10:00")
		LocalDateTime createdAt
	) {

		public static ChatMessageResponse from(ChatMessage message, Long currentUserId) {
			return new ChatMessageResponse(
				message.getId(),
				message.getChatRoom().getId(),
				message.getSender().getId(),
				message.getSender().getName(),
				message.getSender().getProfileImageUrl(),
				message.getMessageType(),
				message.getContent(),
				message.getFileUrl(),
				message.getOriginalFileName(),
				message.getFileSize(),
				message.getFileContentType(),
				message.getSender().getId().equals(currentUserId),
				message.getCreatedAt()
			);
		}
	}
}
