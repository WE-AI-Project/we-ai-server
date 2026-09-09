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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 프로젝트 전체 팀원이 공유하는 자료실(Shared Library)의 문서 한 건. */
@Getter
@Entity
@Table(
	name = "project_library_resources",
	indexes = {
		@Index(name = "idx_project_library_resources_project_deleted", columnList = "project_id, deleted_at"),
		@Index(name = "idx_project_library_resources_project_category", columnList = "project_id, category, deleted_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectLibraryResource extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploader_id", nullable = false)
	private User uploader;

	@Column(nullable = false, length = 255)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LibraryResourceCategory category;

	@Column(length = 1000)
	private String description;

	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFileName;

	@Column(name = "stored_file_name", nullable = false, length = 255)
	private String storedFileName;

	@Column(name = "file_url", nullable = false, length = 500)
	private String fileUrl;

	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	@Column(name = "file_content_type", length = 100)
	private String fileContentType;

	@Column(nullable = false, length = 20)
	private String extension;

	@Column(name = "view_count", nullable = false)
	@Builder.Default
	private long viewCount = 0L;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static ProjectLibraryResource uploaded(
		Project project,
		User uploader,
		String title,
		LibraryResourceCategory category,
		String description,
		String originalFileName,
		String storedFileName,
		String fileUrl,
		Long fileSize,
		String fileContentType,
		String extension
	) {
		return ProjectLibraryResource.builder()
			.project(project)
			.uploader(uploader)
			.title(title)
			.category(category)
			.description(description)
			.originalFileName(originalFileName)
			.storedFileName(storedFileName)
			.fileUrl(fileUrl)
			.fileSize(fileSize)
			.fileContentType(fileContentType)
			.extension(extension)
			.viewCount(0L)
			.build();
	}

	public void increaseViewCount() {
		this.viewCount = this.viewCount + 1;
	}

	public void delete(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
}
