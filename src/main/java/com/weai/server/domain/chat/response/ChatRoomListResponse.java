package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatMessage;
import com.weai.server.domain.chat.domain.ChatRoom;
import com.weai.server.domain.chat.domain.ChatRoomType;
import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "채팅방 목록 조회 응답")
public record ChatRoomListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "필터 조건에 맞는 전체 채팅방 수", example = "3")
	long totalCount,

	@Schema(description = "현재 페이지 번호", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 페이지 수", example = "1")
	int totalPages,

	@ArraySchema(schema = @Schema(implementation = ChatRoomResponse.class))
	List<ChatRoomResponse> chatRooms
) {

	public static ChatRoomListResponse from(Long projectId, Page<ChatRoomResponse> chatRoomPage) {
		return new ChatRoomListResponse(
			projectId,
			chatRoomPage.getTotalElements(),
			chatRoomPage.getNumber(),
			chatRoomPage.getSize(),
			chatRoomPage.getTotalPages(),
			chatRoomPage.getContent()
		);
	}

	@Schema(description = "채팅방 목록 아이템")
	public record ChatRoomResponse(
		@Schema(description = "채팅방 ID", example = "1")
		Long chatRoomId,

		@Schema(description = "프로젝트 ID", example = "1")
		Long projectId,

		@Schema(description = "채팅방 이름", example = "전체 채팅")
		String name,

		@Schema(description = "채팅방 설명", example = "프로젝트 전체 멤버가 참여하는 채팅방입니다.")
		String description,

		@Schema(description = "채팅방 유형", example = "GENERAL")
		ChatRoomType type,

		@Schema(description = "부서", example = "BACKEND")
		ProjectDepartment department,

		@Schema(description = "비공개 여부", example = "false")
		boolean isPrivate,

		@Schema(description = "채팅방 멤버 수", example = "6")
		long memberCount,

		@Schema(description = "읽지 않은 메시지 수", example = "2")
		long unreadCount,

		@Schema(description = "마지막 메시지")
		LastMessageResponse lastMessage,

		@Schema(description = "생성 시각", example = "2026-05-20T10:00:00")
		LocalDateTime createdAt,

		@Schema(description = "수정 시각", example = "2026-05-25T14:20:00")
		LocalDateTime updatedAt
	) {

		public static ChatRoomResponse from(
			ChatRoom chatRoom,
			long memberCount,
			long unreadCount,
			ChatMessage lastMessage
		) {
			return new ChatRoomResponse(
				chatRoom.getId(),
				chatRoom.getProject().getId(),
				chatRoom.getName(),
				chatRoom.getDescription(),
				chatRoom.getType(),
				chatRoom.getDepartment(),
				chatRoom.isPrivate(),
				memberCount,
				unreadCount,
				lastMessage == null ? null : LastMessageResponse.from(lastMessage),
				chatRoom.getCreatedAt(),
				chatRoom.getUpdatedAt()
			);
		}
	}

	@Schema(description = "마지막 메시지 정보")
	public record LastMessageResponse(
		@Schema(description = "메시지 ID", example = "101")
		Long messageId,

		@Schema(description = "메시지 내용", example = "오늘 일정 API 작업 완료했습니다.")
		String content,

		@Schema(description = "발신자 ID", example = "3")
		Long senderId,

		@Schema(description = "발신자 이름", example = "김민혁")
		String senderName,

		@Schema(description = "메시지 생성 시각", example = "2026-05-25T14:20:00")
		LocalDateTime createdAt
	) {

		public static LastMessageResponse from(ChatMessage chatMessage) {
			return new LastMessageResponse(
				chatMessage.getId(),
				chatMessage.getContent(),
				chatMessage.getSender().getId(),
				chatMessage.getSender().getName(),
				chatMessage.getCreatedAt()
			);
		}
	}
}
