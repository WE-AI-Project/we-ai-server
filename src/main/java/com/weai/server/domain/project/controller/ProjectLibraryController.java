package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.domain.LibraryResourceCategory;
import com.weai.server.domain.project.response.LibraryResourceListResponse;
import com.weai.server.domain.project.response.LibraryResourceResponse;
import com.weai.server.domain.project.service.ProjectLibraryService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Project Library", description = "프로젝트 팀 전체가 공유하는 자료실(Shared Library) API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/library")
public class ProjectLibraryController {

	private final ProjectLibraryService projectLibraryService;

	@Operation(summary = "공유 자료 업로드", description = "제목/카테고리/설명과 함께 문서 파일을 multipart/form-data로 업로드합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.LIBRARY_TITLE_REQUIRED,
		ErrorCode.LIBRARY_FILE_REQUIRED,
		ErrorCode.LIBRARY_FILE_EMPTY,
		ErrorCode.LIBRARY_FILE_SIZE_EXCEEDED,
		ErrorCode.LIBRARY_FILE_TYPE_NOT_ALLOWED,
		ErrorCode.LIBRARY_UPLOAD_FAILED
	})
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<LibraryResourceResponse> upload(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "업로드할 문서 파일") @RequestParam(required = false) MultipartFile file,
		@Parameter(description = "제목") @RequestParam String title,
		@Parameter(description = "카테고리") @RequestParam(required = false) LibraryResourceCategory category,
		@Parameter(description = "설명") @RequestParam(required = false) String description
	) {
		return ApiResponse.success(
			"LIBRARY_UPLOAD_SUCCESS",
			"공유 자료가 업로드되었습니다.",
			projectLibraryService.upload(authentication.getName(), projectId, file, title, category, description)
		);
	}

	@Operation(summary = "공유 자료 목록 조회", description = "프로젝트의 공유 자료를 최신순으로 조회합니다. category 필터를 선택적으로 사용할 수 있습니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping
	public ApiResponse<LibraryResourceListResponse> getResources(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
		@Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
		@Parameter(description = "카테고리 필터") @RequestParam(required = false) LibraryResourceCategory category
	) {
		return ApiResponse.success(
			"LIBRARY_LIST_SUCCESS",
			"공유 자료 목록 조회에 성공했습니다.",
			projectLibraryService.getResources(authentication.getName(), projectId, page, size, category)
		);
	}

	@Operation(summary = "공유 자료 조회수 증가", description = "자료를 열람/다운로드할 때 호출하여 실제 조회수를 1 증가시킵니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.LIBRARY_RESOURCE_NOT_FOUND
	})
	@PostMapping("/{resourceId}/view")
	public ApiResponse<LibraryResourceResponse> view(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "자료 ID") @PathVariable Long resourceId
	) {
		return ApiResponse.success(
			"LIBRARY_VIEW_SUCCESS",
			"조회수가 증가했습니다.",
			projectLibraryService.view(authentication.getName(), projectId, resourceId)
		);
	}

	@Operation(summary = "공유 자료 삭제", description = "공유 자료를 소프트 삭제합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.LIBRARY_RESOURCE_NOT_FOUND
	})
	@DeleteMapping("/{resourceId}")
	public ApiResponse<Void> delete(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "자료 ID") @PathVariable Long resourceId
	) {
		projectLibraryService.delete(authentication.getName(), projectId, resourceId);
		return ApiResponse.successMessage("LIBRARY_DELETE_SUCCESS", "공유 자료가 삭제되었습니다.");
	}
}
