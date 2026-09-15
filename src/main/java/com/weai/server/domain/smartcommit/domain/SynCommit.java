package com.weai.server.domain.smartcommit.domain;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A "syn commit": SYNAIPSE's own DB-persisted, no-git commit record. Produced by
 * {@code SynCommitAiService} from a batch of drained {@link SynPendingChange} rows, either
 * automatically (idle scheduler) or on demand ("syn commit" API call).
 */
@Getter
@Entity
@Table(
	name = "syn_commits",
	indexes = {
		@Index(name = "idx_syn_commits_project_committed", columnList = "project_id, committed_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SynCommit extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SynCommitType type;

	@Column(name = "commit_message", nullable = false, length = 500)
	private String commitMessage;

	@Lob
	@Column(nullable = false)
	private String summary;

	@Column(name = "changed_file_count", nullable = false)
	private int changedFileCount;

	@Column(name = "committed_at", nullable = false)
	private LocalDateTime committedAt;

	public static SynCommit create(
		Project project,
		SynCommitType type,
		String commitMessage,
		String summary,
		int changedFileCount,
		LocalDateTime committedAt
	) {
		return SynCommit.builder()
			.project(project)
			.type(type)
			.commitMessage(commitMessage)
			.summary(summary)
			.changedFileCount(changedFileCount)
			.committedAt(committedAt)
			.build();
	}
}
