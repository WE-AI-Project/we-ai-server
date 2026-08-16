package com.weai.server.domain.chat.response;

import com.weai.server.domain.chat.domain.ChatDocument;
import com.weai.server.domain.chat.domain.DocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "문서 업로드 응답")
public record DocumentUploadResponse(
	@Schema(description = "문서 ID", example = "1")
	Long documentId,

	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "원본 파일명", example = "회의자료.pdf")
	String originalFileName,

	@Schema(description = "파일 URL", example = "/uploads/projects/1/documents/uuid.pdf")
	String fileUrl,

	@Schema(description = "파일 크기", example = "204800")
	Long fileSize,

	@Schema(description = "파일 Content-Type", example = "application/pdf")
	String fileContentType,

	@Schema(description = "확장자", example = "pdf")
	String extension,

	@Schema(description = "문서 상태", example = "UPLOADED")
	DocumentStatus status,

	@Schema(description = "생성 일시", example = "2026-08-16T14:10:00")
	LocalDateTime createdAt
) {

	public static DocumentUploadResponse from(ChatDocument document) {
		return new DocumentUploadResponse(
			document.getId(),
			document.getProject().getId(),
			document.getOriginalFileName(),
			document.getFileUrl(),
			document.getFileSize(),
			document.getFileContentType(),
			document.getExtension(),
			document.getStatus(),
			document.getCreatedAt()
		);
	}
}
