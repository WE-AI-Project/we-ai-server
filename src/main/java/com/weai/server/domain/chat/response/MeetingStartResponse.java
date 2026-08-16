package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatMeeting;
import com.weai.server.domain.chat.domain.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "회의 모드 시작 응답")
public record MeetingStartResponse(
	@Schema(description = "회의 ID", example = "1")
	Long meetingId,

	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "채팅방 ID", example = "1")
	Long chatRoomId,

	@Schema(description = "회의 제목", example = "백엔드 API 회의")
	String title,

	@Schema(description = "회의 설명", example = "채팅 문서 및 회의록 기능 논의")
	String description,

	@Schema(description = "호스트 userId", example = "3")
	Long hostUserId,

	@Schema(description = "호스트 이름", example = "김민혁")
	String hostUserName,

	@Schema(description = "회의 상태", example = "IN_PROGRESS")
	MeetingStatus status,

	@Schema(description = "회의 시작 일시", example = "2026-08-16T14:30:00")
	LocalDateTime startedAt
) {

	public static MeetingStartResponse from(ChatMeeting meeting) {
		return new MeetingStartResponse(
			meeting.getId(),
			meeting.getProject().getId(),
			meeting.getChatRoom() == null ? null : meeting.getChatRoom().getId(),
			meeting.getTitle(),
			meeting.getDescription(),
			meeting.getHostUser().getId(),
			meeting.getHostUser().getName(),
			meeting.getStatus(),
			meeting.getStartedAt()
		);
	}
}
