package com.weai.server.domain.chat.domain;

import com.weai.server.domain.project.domain.Project;
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

@Getter
@Entity
@Table(
	name = "chat_documents",
	indexes = {
		@Index(name = "idx_chat_documents_project_status", columnList = "project_id,status"),
		@Index(name = "idx_chat_documents_project_created", columnList = "project_id,created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatDocument extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "document_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploader_id", nullable = false)
	private User uploader;

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

	@Column(length = 500)
	private String description;

	@Column(name = "extracted_text", columnDefinition = "LONGTEXT")
	private String extractedText;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private DocumentStatus status;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static ChatDocument uploaded(
		Project project,
		User uploader,
		String originalFileName,
		String storedFileName,
		String fileUrl,
		Long fileSize,
		String fileContentType,
		String extension,
		String description,
		String extractedText
	) {
		return ChatDocument.builder()
			.project(project)
			.uploader(uploader)
			.originalFileName(originalFileName)
			.storedFileName(storedFileName)
			.fileUrl(fileUrl)
			.fileSize(fileSize)
			.fileContentType(fileContentType)
			.extension(extension)
			.description(description)
			.extractedText(extractedText)
			.status(DocumentStatus.UPLOADED)
			.build();
	}

	public void markBriefingCreated() {
		this.status = DocumentStatus.BRIEFING_CREATED;
	}
}
