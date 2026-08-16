package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatMeeting;
import com.weai.server.domain.chat.domain.MeetingMinute;
import com.weai.server.domain.chat.domain.MeetingStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "회의 모드 종료 및 회의록 저장 응답")
public record MeetingEndResponse(
	@Schema(description = "회의 ID", example = "1")
	Long meetingId,

	@Schema(description = "회의록 ID", example = "1")
	Long minuteId,

	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "회의 제목", example = "백엔드 API 회의")
	String title,

	@Schema(description = "회의록 원문", example = "오늘 회의에서는 문서 업로드 API와 회의록 저장 API 구현 범위를 정리하였다.")
	String content,

	@Schema(description = "회의 요약", example = "채팅 문서/회의 기능 구현 범위를 확정하였다.")
	String summary,

	@ArraySchema(schema = @Schema(description = "해야 할 일", example = "문서 업로드 API 구현"))
	List<String> actionItems,

	@Schema(description = "회의 상태", example = "ENDED")
	MeetingStatus status,

	@Schema(description = "회의 시작 일시", example = "2026-08-16T14:30:00")
	LocalDateTime startedAt,

	@Schema(description = "회의 종료 일시", example = "2026-08-16T15:00:00")
	LocalDateTime endedAt,

	@Schema(description = "회의록 생성 일시", example = "2026-08-16T15:00:00")
	LocalDateTime createdAt
) {

	public static MeetingEndResponse from(ChatMeeting meeting, MeetingMinute minute, List<String> actionItems) {
		return new MeetingEndResponse(
			meeting.getId(),
			minute.getId(),
			meeting.getProject().getId(),
			meeting.getTitle(),
			minute.getContent(),
			minute.getSummary(),
			actionItems,
			meeting.getStatus(),
			meeting.getStartedAt(),
			meeting.getEndedAt(),
			minute.getCreatedAt()
		);
	}
}
