package com.weai.server.domain.project.domain;

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
	name = "server_logs",
	indexes = {
		@Index(name = "idx_server_logs_project_created_at", columnList = "project_id, created_at"),
		@Index(name = "idx_server_logs_project_deleted_at", columnList = "project_id, deleted_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ServerLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ServerLogLevel level;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ServerLogSource source;

	@Column(nullable = false, length = 4000)
	private String message;

	@Column(name = "thread_name", length = 255)
	private String threadName;

	@Column(name = "logger_name", length = 500)
	private String loggerName;

	@Column(name = "trace_id", length = 255)
	private String traceId;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static ServerLog create(
		Project project,
		ServerLogLevel level,
		ServerLogSource source,
		String message,
		String threadName,
		String loggerName,
		String traceId
	) {
		return ServerLog.builder()
			.project(project)
			.level(level)
			.source(source)
			.message(message)
			.threadName(threadName)
			.loggerName(loggerName)
			.traceId(traceId)
			.build();
	}

	public void delete(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
}
