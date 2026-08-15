package com.weai.server.domain.ai.qa.response;

import com.weai.server.domain.ai.qa.domain.QaIssueSeverity;
import com.weai.server.domain.ai.qa.domain.QaIssueStatus;
import com.weai.server.domain.ai.qa.domain.QaReport;
import com.weai.server.domain.ai.qa.domain.QaReportIssue;
import com.weai.server.domain.ai.qa.domain.QaReportStatus;
import com.weai.server.domain.ai.qa.domain.QaReportTestResult;
import com.weai.server.domain.ai.qa.domain.QaTestStatus;
import com.weai.server.domain.ai.qa.domain.QaTestType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Schema(description = "QA 리포트 상세 조회 응답")
public record QaReportDetailResponse(
	@Schema(description = "QA 리포트 ID", example = "1")
	Long qaReportId,

	@Schema(description = "QA 실행 ID", example = "1")
	Long qaRunId,

	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "커밋 ID", example = "10")
	String commitId,

	@Schema(description = "커밋 메시지", example = "feat: 일정 API 구현")
	String commitMessage,

	@Schema(description = "QA 리포트 상태", example = "SUCCESS")
	QaReportStatus status,

	@Schema(description = "QA 요약", example = "전체적으로 정상이며 일부 개선사항이 발견되었습니다.")
	String summary,

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

	@ArraySchema(schema = @Schema(implementation = QaIssueResponse.class))
	List<QaIssueResponse> issues,

	@ArraySchema(schema = @Schema(implementation = QaTestResultResponse.class))
	List<QaTestResultResponse> testResults,

	@Schema(description = "생성 일시", example = "2026-08-15T14:20:00")
	LocalDateTime createdAt
) {

	public static QaReportDetailResponse from(QaReport report, List<QaReportTestResult> testResults) {
		return new QaReportDetailResponse(
			report.getId(),
			report.getQaRun() == null ? null : report.getQaRun().getId(),
			report.getProject().getId(),
			report.getCommitId(),
			report.getCommitMessage(),
			report.getStatus(),
			report.getSummary(),
			report.getTotalIssueCount(),
			report.getCriticalCount(),
			report.getMajorCount(),
			report.getMinorCount(),
			report.getTestPassCount(),
			report.getTestFailCount(),
			report.getIssues().stream()
				.sorted(Comparator.comparing(QaReportIssue::getId))
				.map(QaIssueResponse::from)
				.toList(),
			testResults.stream()
				.map(QaTestResultResponse::from)
				.toList(),
			report.getCreatedAt()
		);
	}

	@Schema(description = "QA 이슈")
	public record QaIssueResponse(
		@Schema(description = "이슈 ID", example = "1")
		Long issueId,

		@Schema(description = "심각도", example = "MAJOR")
		QaIssueSeverity severity,

		@Schema(description = "제목", example = "예외 처리 누락")
		String title,

		@Schema(description = "설명", example = "날짜 검증 실패 시 명확한 예외 메시지가 필요합니다.")
		String description,

		@Schema(description = "파일 경로", example = "ProjectScheduleService.java")
		String filePath,

		@Schema(description = "라인 번호", example = "82")
		Integer lineNumber,

		@Schema(description = "개선 제안", example = "INVALID_SCHEDULE_DATE 예외를 반환하세요.")
		String suggestion,

		@Schema(description = "이슈 상태", example = "OPEN")
		QaIssueStatus status
	) {

		private static QaIssueResponse from(QaReportIssue issue) {
			return new QaIssueResponse(
				issue.getId(),
				issue.getSeverity(),
				issue.getTitle(),
				issue.getDescription(),
				issue.getFilePath(),
				issue.getLineNumber(),
				issue.getSuggestion(),
				issue.getStatus()
			);
		}
	}

	@Schema(description = "QA 테스트 결과")
	public record QaTestResultResponse(
		@Schema(description = "테스트 이름", example = "일정 생성 성공 테스트")
		String testName,

		@Schema(description = "테스트 유형", example = "UNIT")
		QaTestType testType,

		@Schema(description = "테스트 상태", example = "PASSED")
		QaTestStatus status,

		@Schema(description = "메시지", example = "정상 통과")
		String message,

		@Schema(description = "실행 시간(ms)", example = "120")
		Long durationMs
	) {

		private static QaTestResultResponse from(QaReportTestResult testResult) {
			return new QaTestResultResponse(
				testResult.getTestName(),
				testResult.getTestType(),
				testResult.getStatus(),
				testResult.getMessage(),
				testResult.getDurationMs()
			);
		}
	}
}
