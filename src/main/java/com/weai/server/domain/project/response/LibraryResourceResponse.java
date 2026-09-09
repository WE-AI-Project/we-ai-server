package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.LibraryResourceCategory;
import com.weai.server.domain.project.domain.ProjectLibraryResource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "공유 자료실 문서")
public record LibraryResourceResponse(
	@Schema(description = "자료 ID", example = "1")
	Long id,

	@Schema(description = "제목", example = "WE&AI REST API Reference")
	String title,

	@Schema(description = "카테고리", example = "DOCS")
	LibraryResourceCategory category,

	@Schema(description = "설명")
	String description,

	@Schema(description = "원본 파일명", example = "api-reference.md")
	String originalFileName,

	@Schema(description = "다운로드 URL", example = "/uploads/projects/1/library/xxxx.md")
	String fileUrl,

	@Schema(description = "파일 크기(byte)", example = "20480")
	Long fileSize,

	@Schema(description = "확장자", example = "md")
	String extension,

	@Schema(description = "업로더 이름", example = "홍길동")
	String uploaderName,

	@Schema(description = "조회수", example = "12")
	long viewCount,

	@Schema(description = "업로드 일시")
	LocalDateTime createdAt
) {
	public static LibraryResourceResponse from(ProjectLibraryResource resource) {
		return new LibraryResourceResponse(
			resource.getId(),
			resource.getTitle(),
			resource.getCategory(),
			resource.getDescription(),
			resource.getOriginalFileName(),
			resource.getFileUrl(),
			resource.getFileSize(),
			resource.getExtension(),
			resource.getUploader().getName(),
			resource.getViewCount(),
			resource.getCreatedAt()
		);
	}
}
