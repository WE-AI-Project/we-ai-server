package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectLibraryResource;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "공유 자료실 목록 조회 응답")
public record LibraryResourceListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "현재 페이지", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 페이지 수", example = "1")
	int totalPages,

	@Schema(description = "전체 자료 수", example = "8")
	long totalCount,

	@ArraySchema(schema = @Schema(implementation = LibraryResourceResponse.class))
	List<LibraryResourceResponse> resources
) {
	public static LibraryResourceListResponse from(Long projectId, Page<ProjectLibraryResource> resources) {
		return new LibraryResourceListResponse(
			projectId,
			resources.getNumber(),
			resources.getSize(),
			resources.getTotalPages(),
			resources.getTotalElements(),
			resources.getContent().stream().map(LibraryResourceResponse::from).toList()
		);
	}
}
