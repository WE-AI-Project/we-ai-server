package com.weai.server.domain.ai.qa.response;

import com.weai.server.domain.ai.qa.domain.QaReport;
import com.weai.server.domain.ai.qa.domain.QaReportStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "QA 리포트 목록 조회 응답")
public record QaReportListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "현재 페이지", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 페이지 수", example = "1")
	int totalPages,

	@Schema(description = "전체 리포트 수", example = "2")
	long totalCount,

	@ArraySchema(schema = @Schema(implementation = QaReportSummaryResponse.class))
	List<QaReportSummaryResponse> reports
) {

	public static QaReportListResponse from(Long projectId, Page<QaReport> reports) {
		return new QaReportListResponse(
			projectId,
			reports.getNumber(),
			reports.getSize(),
			reports.getTotalPages(),
			reports.getTotalElements(),
			reports.getContent().stream()
				.map(QaReportSummaryResponse::from)
				.toList()
		);
	}

	@Schema(description = "QA 리포트 요약")
	public record QaReportSummaryResponse(
		@Schema(description = "QA 리포트 ID", example = "1")
		Long qaReportId,

		@Schema(description = "QA 실행 ID", example = "1")
		Long qaRunId,

		@Schema(description = "커밋 ID", example = "10")
		String commitId,

		@Schema(description = "커밋 메시지", example = "feat: 일정 API 구현")
		String commitMessage,

		@Schema(description = "QA 리포트 상태", example = "SUCCESS")
		QaReportStatus status,

		@Schema(description = "전체 이슈 수", example = "3")
		int totalIssueCount,

		@Schema(description = "치명 이슈 수", example = "0")
		int criticalCount,

		@Schema(description = "주요 이슈 수", example = "1")
		int majorCount,

		@Schema(description = "경미 이슈 수", example = "2")
		int minorCount,

		@Schema(description = "통과 테스트 수", example = "12")
		int testPassCount,

		@Schema(description = "실패 테스트 수", example = "0")
		int testFailCount,

		@Schema(description = "생성 일시", example = "2026-08-15T14:20:00")
		LocalDateTime createdAt
	) {

		public static QaReportSummaryResponse from(QaReport report) {
			return new QaReportSummaryResponse(
				report.getId(),
				report.getQaRun() == null ? null : report.getQaRun().getId(),
				report.getCommitId(),
				report.getCommitMessage(),
				report.getStatus(),
				report.getTotalIssueCount(),
				report.getCriticalCount(),
				report.getMajorCount(),
				report.getMinorCount(),
				report.getTestPassCount(),
				report.getTestFailCount(),
				report.getCreatedAt()
			);
		}
	}
}
