package com.weai.server.domain.ai.qa.domain;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "qa_reports",
	indexes = {
		@Index(name = "idx_qa_reports_project_created", columnList = "project_id,created_at"),
		@Index(name = "idx_qa_reports_project_commit", columnList = "project_id,commit_id"),
		@Index(name = "idx_qa_reports_project_status", columnList = "project_id,status")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class QaReport extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "qa_run_id")
	private QaRun qaRun;

	@Column(name = "commit_id", length = 100)
	private String commitId;

	@Column(name = "commit_hash", length = 100)
	private String commitHash;

	@Column(name = "commit_message", length = 500)
	private String commitMessage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QaReportStatus status;

	@Column(length = 2000)
	private String summary;

	@Column(name = "total_issue_count", nullable = false)
	private int totalIssueCount;

	@Column(name = "critical_count", nullable = false)
	private int criticalCount;

	@Column(name = "major_count", nullable = false)
	private int majorCount;

	@Column(name = "minor_count", nullable = false)
	private int minorCount;

	@Column(name = "test_pass_count", nullable = false)
	private int testPassCount;

	@Column(name = "test_fail_count", nullable = false)
	private int testFailCount;

	@Builder.Default
	@OneToMany(mappedBy = "qaReport", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<QaReportIssue> issues = new ArrayList<>();

	@Builder.Default
	@OneToMany(mappedBy = "qaReport", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<QaReportTestResult> testResults = new ArrayList<>();
}
