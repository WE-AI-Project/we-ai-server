package com.weai.server.domain.ai.qa.response;

import com.weai.server.domain.ai.qa.domain.QaReport;
import com.weai.server.domain.ai.qa.domain.QaReportStatus;
import com.weai.server.domain.project.response.ProjectCommitDetailResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "커밋별 QA 결과 조회 응답")
public record CommitQaResultResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "커밋 ID", example = "32a4ffc")
	String commitId,

	@Schema(description = "커밋 해시", example = "32a4ffcb47ed8b98168f0d117f41fcbec5c2bb08")
	String commitHash,

	@Schema(description = "커밋 메시지", example = "feat: 일정 API 구현")
	String commitMessage,

	@Schema(description = "최신 QA 리포트")
	CommitQaLatestReportResponse latestReport,

	@ArraySchema(schema = @Schema(implementation = CommitQaReportResponse.class))
	List<CommitQaReportResponse> reports
) {

	public static CommitQaResultResponse from(ProjectCommitDetailResponse commit, List<QaReport> reports) {
		return new CommitQaResultResponse(
			commit.projectId(),
			commit.shortCommitHash(),
			commit.commitHash(),
			commit.message(),
			reports.isEmpty() ? null : CommitQaLatestReportResponse.from(reports.get(0)),
			reports.stream()
				.map(CommitQaReportResponse::from)
				.toList()
		);
	}

	@Schema(description = "최신 커밋 QA 리포트")
	public record CommitQaLatestReportResponse(
		@Schema(description = "QA 리포트 ID", example = "1")
		Long qaReportId,

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

		private static CommitQaLatestReportResponse from(QaReport report) {
			return new CommitQaLatestReportResponse(
				report.getId(),
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

	@Schema(description = "커밋 QA 리포트 요약")
	public record CommitQaReportResponse(
		@Schema(description = "QA 리포트 ID", example = "1")
		Long qaReportId,

		@Schema(description = "QA 리포트 상태", example = "SUCCESS")
		QaReportStatus status,

		@Schema(description = "전체 이슈 수", example = "3")
		int totalIssueCount,

		@Schema(description = "생성 일시", example = "2026-08-15T14:20:00")
		LocalDateTime createdAt
	) {

		private static CommitQaReportResponse from(QaReport report) {
			return new CommitQaReportResponse(
				report.getId(),
				report.getStatus(),
				report.getTotalIssueCount(),
				report.getCreatedAt()
			);
		}
	}
}
