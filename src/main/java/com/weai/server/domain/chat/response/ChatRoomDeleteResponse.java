package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatRoom;
import com.weai.server.domain.chat.domain.ChatRoomType;
import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅방 삭제 응답")
public record ChatRoomDeleteResponse(
	@Schema(description = "삭제한 채팅방 ID", example = "15")
	Long chatRoomId,

	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "채팅방 이름", example = "백엔드 채팅방")
	String name,

	@Schema(description = "채팅방 유형", example = "DEPARTMENT")
	ChatRoomType type,

	@Schema(description = "채팅방 부서", example = "BACKEND", nullable = true)
	ProjectDepartment department,

	@Schema(description = "삭제 시각", example = "2026-09-29T12:00:00")
	LocalDateTime deletedAt
) {

	public static ChatRoomDeleteResponse from(ChatRoom chatRoom) {
		return new ChatRoomDeleteResponse(
			chatRoom.getId(),
			chatRoom.getProject().getId(),
			chatRoom.getName(),
			chatRoom.getType(),
			chatRoom.getDepartment(),
			chatRoom.getDeletedAt()
		);
	}
}
