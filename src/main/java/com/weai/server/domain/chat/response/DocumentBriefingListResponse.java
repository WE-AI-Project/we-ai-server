package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.BriefingStatus;
import com.weai.server.domain.chat.domain.DocumentBriefing;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "문서 브리핑 목록 조회 응답")
public record DocumentBriefingListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "현재 페이지", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 페이지 수", example = "1")
	int totalPages,

	@Schema(description = "전체 브리핑 수", example = "2")
	long totalCount,

	@ArraySchema(schema = @Schema(implementation = BriefingSummaryResponse.class))
	List<BriefingSummaryResponse> briefings
) {

	public static DocumentBriefingListResponse from(Long projectId, Page<BriefingSummaryResponse> page) {
		return new DocumentBriefingListResponse(
			projectId,
			page.getNumber(),
			page.getSize(),
			page.getTotalPages(),
			page.getTotalElements(),
			page.getContent()
		);
	}

	@Schema(description = "문서 브리핑 목록 항목")
	public record BriefingSummaryResponse(
		@Schema(description = "브리핑 ID", example = "1")
		Long briefingId,

		@Schema(description = "문서 ID", example = "1")
		Long documentId,

		@Schema(description = "문서명", example = "회의자료.pdf")
		String documentName,

		@Schema(description = "요약", example = "문서의 핵심 내용을 요약한 결과입니다.")
		String summary,

		@ArraySchema(schema = @Schema(description = "핵심 포인트", example = "프로젝트 일정 관리 기능 보완 필요"))
		List<String> keyPoints,

		@Schema(description = "생성자 ID", example = "3")
		Long creatorId,

		@Schema(description = "생성자 이름", example = "김민혁")
		String creatorName,

		@Schema(description = "브리핑 상태", example = "COMPLETED")
		BriefingStatus status,

		@Schema(description = "생성 일시", example = "2026-08-16T14:20:00")
		LocalDateTime createdAt
	) {

		public static BriefingSummaryResponse from(DocumentBriefing briefing, List<String> keyPoints) {
			return new BriefingSummaryResponse(
				briefing.getId(),
				briefing.getDocument().getId(),
				briefing.getDocument().getOriginalFileName(),
				briefing.getSummary(),
				keyPoints,
				briefing.getCreator().getId(),
				briefing.getCreator().getName(),
				briefing.getStatus(),
				briefing.getCreatedAt()
			);
		}
	}
}
