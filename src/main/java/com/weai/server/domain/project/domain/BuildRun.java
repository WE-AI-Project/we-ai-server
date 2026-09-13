package com.weai.server.domain.project.domain;

import com.weai.server.domain.user.domain.User;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "build_runs",
	indexes = {
		@Index(name = "idx_build_runs_project_created_at", columnList = "project_id, created_at"),
		@Index(name = "idx_build_runs_project_status", columnList = "project_id, status")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BuildRun extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "build_run_id")
	private Long buildRunId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_id", nullable = false)
	private User requester;

	@Column(name = "task_name", nullable = false, length = 50)
	private String taskName;

	@Enumerated(EnumType.STRING)
	@Column(name = "build_tool", nullable = false, length = 20)
	private BuildTool buildTool;

	@Column(length = 30)
	private String profile;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BuildRunStatus status;

	@Column(nullable = false, length = 500)
	private String command;

	@Column(name = "exit_code")
	private Integer exitCode;

	@Lob
	@Column(columnDefinition = "TEXT")
	private String output;

	@Lob
	@Column(name = "error_output", columnDefinition = "TEXT")
	private String errorOutput;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	@Column(name = "duration_ms")
	private Long durationMs;

	public static BuildRun start(
		Project project,
		User requester,
		String taskName,
		BuildTool buildTool,
		String profile,
		String command
	) {
		LocalDateTime now = LocalDateTime.now();
		return BuildRun.builder()
			.project(project)
			.requester(requester)
			.taskName(taskName)
			.buildTool(buildTool)
			.profile(profile)
			.status(BuildRunStatus.RUNNING)
			.command(command)
			.startedAt(now)
			.output("")
			.errorOutput("")
			.build();
	}

	public void succeed(int exitCode, String output, String errorOutput, LocalDateTime finishedAt) {
		finish(BuildRunStatus.SUCCESS, exitCode, output, errorOutput, finishedAt);
	}

	public void fail(int exitCode, String output, String errorOutput, LocalDateTime finishedAt) {
		finish(BuildRunStatus.FAILED, exitCode, output, errorOutput, finishedAt);
	}

	private void finish(BuildRunStatus status, int exitCode, String output, String errorOutput, LocalDateTime finishedAt) {
		this.status = status;
		this.exitCode = exitCode;
		this.output = output;
		this.errorOutput = errorOutput;
		this.finishedAt = finishedAt;
		this.durationMs = startedAt == null ? null : Duration.between(startedAt, finishedAt).toMillis();
	}
}
