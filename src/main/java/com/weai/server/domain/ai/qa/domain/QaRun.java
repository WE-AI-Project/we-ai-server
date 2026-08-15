package com.weai.server.domain.ai.qa.domain;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.global.entity.BaseEntity;
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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "qa_runs",
	indexes = {
		@Index(name = "idx_qa_runs_project_status", columnList = "project_id,status"),
		@Index(name = "idx_qa_runs_project_commit", columnList = "project_id,commit_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class QaRun extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "commit_id", length = 100)
	private String commitId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QaRunStatus status;

	@Column(name = "progress_rate", nullable = false)
	private int progressRate;

	@Column(name = "current_step")
	private Integer currentStep;

	@Column(name = "total_step")
	private Integer totalStep;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;
}
