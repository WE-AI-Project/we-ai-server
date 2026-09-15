package com.weai.server.domain.ai.qa;

import com.weai.server.domain.ai.qa.domain.QaIssueSeverity;
import com.weai.server.domain.ai.qa.domain.QaIssueStatus;
import com.weai.server.domain.ai.qa.domain.QaReport;
import com.weai.server.domain.ai.qa.domain.QaReportIssue;
import com.weai.server.domain.ai.qa.domain.QaReportStatus;
import com.weai.server.domain.ai.qa.domain.QaRun;
import com.weai.server.domain.ai.qa.domain.QaRunStatus;
import com.weai.server.domain.ai.qa.repository.QaReportRepository;
import com.weai.server.domain.ai.qa.repository.QaRunRepository;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.repository.ProjectRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Turns a single {@link AiQaService#analyze} result into a persisted {@link QaRun}/{@link QaReport}
 * so the existing read-only QA history/report API (QaQueryService, ProjectQaQueryController,
 * CommitQaQueryController) actually has data to return instead of being permanently empty.
 *
 * The AI QA model returns one free-text bug report and one optimization suggestion per call
 * (not a structured list of findings), so each analysis is recorded as exactly one
 * {@link QaReportIssue} — this is an honest 1:1 mapping of what the model actually produced,
 * not a fabricated multi-issue breakdown.
 */
@Service
@RequiredArgsConstructor
public class QaPersistenceService {

	private static final Set<String> CRITICAL_KEYWORDS = Set.of(
		"보안", "취약", "유출", "노출", "critical", "security", "vulnerability", "exploit", "인증 우회"
	);

	private static final int SUMMARY_MAX_LENGTH = 2000;
	private static final int TITLE_MAX_LENGTH = 200;
	private static final int COMMIT_MESSAGE_MAX_LENGTH = 500;

	private final ProjectRepository projectRepository;
	private final QaRunRepository qaRunRepository;
	private final QaReportRepository qaReportRepository;

	@Transactional
	public QaReport persist(Long projectId, String commitId, QaResponse response) {
		Project project = projectRepository.getReferenceById(projectId);
		LocalDateTime now = LocalDateTime.now();
		String normalizedCommitId = StringUtils.hasText(commitId) ? commitId.trim() : null;

		QaRun qaRun = qaRunRepository.save(QaRun.builder()
			.project(project)
			.commitId(normalizedCommitId)
			.status(QaRunStatus.SUCCESS)
			.progressRate(100)
			.currentStep(1)
			.totalStep(1)
			.startedAt(now)
			.finishedAt(now)
			.build());

		QaIssueSeverity severity = classifySeverity(response.bugReport());

		QaReport report = QaReport.builder()
			.project(project)
			.qaRun(qaRun)
			.commitId(normalizedCommitId)
			.commitMessage(truncate(response.commitMsg(), COMMIT_MESSAGE_MAX_LENGTH))
			.status(QaReportStatus.SUCCESS)
			.summary(truncate(response.bugReport(), SUMMARY_MAX_LENGTH))
			.totalIssueCount(1)
			.criticalCount(severity == QaIssueSeverity.CRITICAL ? 1 : 0)
			.majorCount(severity == QaIssueSeverity.MAJOR ? 1 : 0)
			.minorCount(severity == QaIssueSeverity.MINOR ? 1 : 0)
			.testPassCount(0)
			.testFailCount(0)
			.build();

		report.getIssues().add(QaReportIssue.builder()
			.qaReport(report)
			.severity(severity)
			.title(truncate(response.bugReport(), TITLE_MAX_LENGTH))
			.description(response.bugReport())
			.suggestion(response.optimization())
			.status(QaIssueStatus.OPEN)
			.build());

		return qaReportRepository.save(report);
	}

	private QaIssueSeverity classifySeverity(String bugReport) {
		if (!StringUtils.hasText(bugReport)) {
			return QaIssueSeverity.MINOR;
		}
		String lower = bugReport.toLowerCase(Locale.ROOT);
		return CRITICAL_KEYWORDS.stream().anyMatch(lower::contains) ? QaIssueSeverity.CRITICAL : QaIssueSeverity.MAJOR;
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
