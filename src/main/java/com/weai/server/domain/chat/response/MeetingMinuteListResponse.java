package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.MeetingMinute;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "회의록 목록 조회 응답")
public record MeetingMinuteListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "현재 페이지", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 페이지 수", example = "1")
	int totalPages,

	@Schema(description = "전체 회의록 수", example = "1")
	long totalCount,

	@ArraySchema(schema = @Schema(implementation = MeetingMinuteSummaryResponse.class))
	List<MeetingMinuteSummaryResponse> minutes
) {

	public static MeetingMinuteListResponse from(Long projectId, Page<MeetingMinuteSummaryResponse> page) {
		return new MeetingMinuteListResponse(
			projectId,
			page.getNumber(),
			page.getSize(),
			page.getTotalPages(),
			page.getTotalElements(),
			page.getContent()
		);
	}

	@Schema(description = "회의록 목록 항목")
	public record MeetingMinuteSummaryResponse(
		@Schema(description = "회의록 ID", example = "1")
		Long minuteId,

		@Schema(description = "회의 ID", example = "1")
		Long meetingId,

		@Schema(description = "회의 제목", example = "백엔드 API 회의")
		String title,

		@Schema(description = "회의 요약", example = "채팅 문서/회의 기능 구현 범위를 확정하였다.")
		String summary,

		@Schema(description = "작성자 ID", example = "3")
		Long writerId,

		@Schema(description = "작성자 이름", example = "김민혁")
		String writerName,

		@Schema(description = "참여자 수", example = "3")
		long participantCount,

		@Schema(description = "회의 시작 일시", example = "2026-08-16T14:30:00")
		LocalDateTime startedAt,

		@Schema(description = "회의 종료 일시", example = "2026-08-16T15:00:00")
		LocalDateTime endedAt,

		@Schema(description = "회의록 생성 일시", example = "2026-08-16T15:00:00")
		LocalDateTime createdAt
	) {

		public static MeetingMinuteSummaryResponse from(MeetingMinute minute, long participantCount) {
			return new MeetingMinuteSummaryResponse(
				minute.getId(),
				minute.getMeeting().getId(),
				minute.getTitle(),
				minute.getSummary(),
				minute.getWriter().getId(),
				minute.getWriter().getName(),
				participantCount,
				minute.getMeeting().getStartedAt(),
				minute.getMeeting().getEndedAt(),
				minute.getCreatedAt()
			);
		}
	}
}
