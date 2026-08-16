package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.BriefingStatus;
import com.weai.server.domain.chat.domain.DocumentBriefing;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "문서 브리핑 생성 응답")
public record DocumentBriefingResponse(
	@Schema(description = "브리핑 ID", example = "1")
	Long briefingId,

	@Schema(description = "문서 ID", example = "1")
	Long documentId,

	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "문서명", example = "회의자료.pdf")
	String documentName,

	@Schema(description = "문서 요약", example = "문서의 핵심 내용을 요약한 결과입니다.")
	String summary,

	@ArraySchema(schema = @Schema(description = "핵심 포인트", example = "프로젝트 일정 관리 기능 보완 필요"))
	List<String> keyPoints,

	@ArraySchema(schema = @Schema(description = "해야 할 일", example = "문서 업로드 API 구현"))
	List<String> actionItems,

	@ArraySchema(schema = @Schema(description = "위험 요소", example = "문서 파싱 라이브러리 미설치 시 PDF 요약 제한"))
	List<String> risks,

	@ArraySchema(schema = @Schema(description = "키워드", example = "문서"))
	List<String> keywords,

	@Schema(description = "브리핑 상태", example = "COMPLETED")
	BriefingStatus status,

	@Schema(description = "생성 일시", example = "2026-08-16T14:20:00")
	LocalDateTime createdAt
) {

	public static DocumentBriefingResponse from(
		DocumentBriefing briefing,
		List<String> keyPoints,
		List<String> actionItems,
		List<String> risks,
		List<String> keywords
	) {
		return new DocumentBriefingResponse(
			briefing.getId(),
			briefing.getDocument().getId(),
			briefing.getProject().getId(),
			briefing.getDocument().getOriginalFileName(),
			briefing.getSummary(),
			keyPoints,
			actionItems,
			risks,
			keywords,
			briefing.getStatus(),
			briefing.getCreatedAt()
		);
	}
}
