package com.weai.server.domain.smartcommit.domain;

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

/** One file's diff that was folded into a {@link SynCommit}, kept so the commit can be inspected later. */
@Getter
@Entity
@Table(
	name = "syn_commit_files",
	indexes = {
		@Index(name = "idx_syn_commit_files_commit", columnList = "syn_commit_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SynCommitFile extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "syn_commit_id", nullable = false)
	private SynCommit synCommit;

	@Column(name = "file_path", nullable = false, length = 500)
	private String filePath;

	@Column(name = "diff_content", nullable = false, columnDefinition = "LONGTEXT")
	private String diffContent;

	public static SynCommitFile create(SynCommit synCommit, String filePath, String diffContent) {
		return SynCommitFile.builder()
			.synCommit(synCommit)
			.filePath(filePath)
			.diffContent(diffContent)
			.build();
	}
}
