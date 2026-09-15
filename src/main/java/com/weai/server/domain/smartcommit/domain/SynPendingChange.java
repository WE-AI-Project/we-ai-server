package com.weai.server.domain.smartcommit.domain;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.user.domain.User;
import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A "syn add"-staged file diff, captured by the VS Code extension and waiting to be folded into a
 * {@link SynCommit}. Stored in the database (rather than an in-memory map) so staged changes
 * survive an app restart and are visible across every server instance.
 */
@Getter
@Entity
@Table(
	name = "syn_pending_changes",
	indexes = {
		@Index(name = "idx_syn_pending_changes_project", columnList = "project_id"),
		@Index(name = "idx_syn_pending_changes_project_path", columnList = "project_id, file_path", unique = true)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SynPendingChange extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "registered_by", nullable = false)
	private User registeredBy;

	@Column(name = "file_path", nullable = false, length = 500)
	private String filePath;

	@Column(name = "diff_content", nullable = false, columnDefinition = "LONGTEXT")
	private String diffContent;

	public static SynPendingChange create(Project project, User registeredBy, String filePath, String diffContent) {
		return SynPendingChange.builder()
			.project(project)
			.registeredBy(registeredBy)
			.filePath(filePath)
			.diffContent(diffContent)
			.build();
	}

	public void updateDiff(User registeredBy, String diffContent) {
		this.registeredBy = registeredBy;
		this.diffContent = diffContent;
	}

	public void mergeAfterFailedCommit(String olderDiffContent) {
		this.diffContent = olderDiffContent + "\n\n--- newer syn-add diff after failed commit ---\n\n" + this.diffContent;
	}
}
