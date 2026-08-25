package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatRoom;
import com.weai.server.domain.chat.domain.ChatRoomType;
import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅방 생성 응답")
public record ChatRoomCreateResponse(
	@Schema(description = "채팅방 ID", example = "15")
	Long chatRoomId,

	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "채팅방 이름", example = "백엔드 채팅방")
	String name,

	@Schema(description = "채팅방 유형", example = "DEPARTMENT")
	ChatRoomType type,

	@Schema(description = "부서", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "프로젝트 기본 채팅방 여부", example = "false")
	boolean isDefault,

	@Schema(description = "활성 채팅방 멤버 수", example = "3")
	long memberCount,

	@Schema(description = "생성자 사용자 ID", example = "1")
	Long createdBy,

	@Schema(description = "생성 시각", example = "2026-08-25T11:50:00")
	LocalDateTime createdAt
) {

	public static ChatRoomCreateResponse from(ChatRoom chatRoom, long memberCount) {
		return new ChatRoomCreateResponse(
			chatRoom.getId(),
			chatRoom.getProject().getId(),
			chatRoom.getName(),
			chatRoom.getType(),
			chatRoom.getDepartment(),
			chatRoom.isDefault(),
			memberCount,
			chatRoom.getCreatedBy().getId(),
			chatRoom.getCreatedAt()
		);
	}
}
