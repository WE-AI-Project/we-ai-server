package com.weai.server.domain.ai.qa;

import static org.assertj.core.api.Assertions.assertThat;

import com.weai.server.domain.ai.qa.domain.QaIssueSeverity;
import com.weai.server.domain.ai.qa.domain.QaReport;
import com.weai.server.domain.ai.qa.domain.QaReportStatus;
import com.weai.server.domain.ai.qa.domain.QaRunStatus;
import com.weai.server.domain.ai.qa.repository.QaReportRepository;
import com.weai.server.domain.ai.qa.repository.QaRunRepository;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QaPersistenceServiceTest {

	@Autowired
	private QaPersistenceService qaPersistenceService;

	@Autowired
	private QaRunRepository qaRunRepository;

	@Autowired
	private QaReportRepository qaReportRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void persistsQaRunAndReportWithOneIssueFromTheAiResponse() {
		Project project = createProject();
		QaResponse response = new QaResponse(
			"Null 검증이 생략되어 NPE가 발생할 수 있습니다.",
			"Optional을 사용하세요.",
			"fix: guard against null user lookup"
		);

		QaReport saved = qaPersistenceService.persist(project.getId(), "abc123", response);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getStatus()).isEqualTo(QaReportStatus.SUCCESS);
		assertThat(saved.getCommitId()).isEqualTo("abc123");
		assertThat(saved.getCommitMessage()).isEqualTo("fix: guard against null user lookup");
		assertThat(saved.getSummary()).isEqualTo(response.bugReport());
		assertThat(saved.getTotalIssueCount()).isEqualTo(1);
		assertThat(saved.getIssues()).hasSize(1);
		assertThat(saved.getIssues().get(0).getDescription()).isEqualTo(response.bugReport());
		assertThat(saved.getIssues().get(0).getSuggestion()).isEqualTo(response.optimization());
		assertThat(saved.getQaRun()).isNotNull();
		assertThat(saved.getQaRun().getStatus()).isEqualTo(QaRunStatus.SUCCESS);
		assertThat(saved.getQaRun().getCommitId()).isEqualTo("abc123");

		// Now-writable QA history means the read API actually finds something.
		List<QaReport> byCommit = qaReportRepository.findByProjectIdAndCommitIdOrderByLatest(project.getId(), "abc123");
		assertThat(byCommit).extracting(QaReport::getId).containsExactly(saved.getId());
	}

	@Test
	void classifiesSecuritySensitiveBugReportsAsCritical() {
		Project project = createProject();
		QaResponse response = new QaResponse(
			"환경 변수에 보안 시크릿이 노출되어 있습니다.",
			"gitignore에 추가하세요.",
			"chore: isolate secret from version control"
		);

		QaReport saved = qaPersistenceService.persist(project.getId(), null, response);

		assertThat(saved.getCriticalCount()).isEqualTo(1);
		assertThat(saved.getMajorCount()).isZero();
		assertThat(saved.getIssues().get(0).getSeverity()).isEqualTo(QaIssueSeverity.CRITICAL);
		assertThat(saved.getCommitId()).isNull();
	}

	@Test
	void blankCommitIdIsStoredAsNullNotAnEmptyString() {
		Project project = createProject();
		QaResponse response = new QaResponse("이슈", "제안", "chore: noop");

		QaReport saved = qaPersistenceService.persist(project.getId(), "   ", response);

		assertThat(saved.getCommitId()).isNull();
		assertThat(saved.getQaRun().getCommitId()).isNull();
	}

	private Project createProject() {
		String suffix = UUID.randomUUID().toString();
		User leader = userRepository.save(User.create(
			"qa-leader-" + suffix,
			"password",
			"Leader",
			"qa-leader-" + suffix + "@example.com",
			UserRole.USER
		));
		return projectRepository.save(Project.create(
			"QA Persistence Project " + suffix.substring(0, 8),
			"QA persistence service test project",
			suffix.substring(0, 8).toUpperCase(),
			"C:\\WE_AI\\qa-persistence-test",
			LocalDate.now(),
			LocalDate.now().plusDays(30),
			leader
		));
	}
}
