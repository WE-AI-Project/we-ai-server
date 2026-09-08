package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatRoomMember;
import com.weai.server.domain.chat.domain.ChatRoomMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 나가기 응답")
public record ChatRoomLeaveResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "채팅방 ID", example = "10")
	Long chatRoomId,

	@Schema(description = "채팅방 멤버 ID", example = "24")
	Long chatRoomMemberId,

	@Schema(description = "나간 사용자 ID", example = "3")
	Long userId,

	@Schema(description = "변경된 멤버 상태", example = "LEFT")
	ChatRoomMemberStatus status
) {

	public static ChatRoomLeaveResponse from(ChatRoomMember member) {
		return new ChatRoomLeaveResponse(
			member.getChatRoom().getProject().getId(),
			member.getChatRoom().getId(),
			member.getId(),
			member.getUser().getId(),
			member.getStatus()
		);
	}
}
