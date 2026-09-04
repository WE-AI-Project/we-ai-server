package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.request.ProjectGitFilePathsRequest;
import com.weai.server.domain.project.response.ProjectGitChangeResponse;
import com.weai.server.domain.project.service.ProjectGitService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Git Changes", description = "프로젝트 Git 변경사항 스테이징 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/changes")
public class ProjectGitChangesController {

	private final ProjectGitService projectGitService;

	@Operation(
		summary = "파일 스테이징",
		description = "선택한 파일을 Git staging area에 추가합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.GIT_REPOSITORY_PATH_NOT_FOUND,
		ErrorCode.GIT_REPOSITORY_NOT_FOUND,
		ErrorCode.GIT_FILE_PATH_REQUIRED,
		ErrorCode.INVALID_GIT_FILE_PATH,
		ErrorCode.GIT_STAGE_FAILED,
		ErrorCode.GIT_COMMAND_EXECUTION_FAILED
	})
	@PostMapping("/stage")
	public ApiResponse<ProjectGitChangeResponse> stageFiles(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestBody(required = false) ProjectGitFilePathsRequest request
	) {
		return ApiResponse.success(
			"GIT_FILE_STAGE_SUCCESS",
			"파일 스테이징에 성공했습니다.",
			projectGitService.stageFiles(authentication.getName(), projectId, request == null ? null : request.filePaths())
		);
	}

	@Operation(
		summary = "파일 언스테이징",
		description = "선택한 파일을 Git staging area에서 제거합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.GIT_REPOSITORY_PATH_NOT_FOUND,
		ErrorCode.GIT_REPOSITORY_NOT_FOUND,
		ErrorCode.GIT_FILE_PATH_REQUIRED,
		ErrorCode.INVALID_GIT_FILE_PATH,
		ErrorCode.GIT_UNSTAGE_FAILED,
		ErrorCode.GIT_COMMAND_EXECUTION_FAILED
	})
	@PostMapping("/unstage")
	public ApiResponse<ProjectGitChangeResponse> unstageFiles(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestBody(required = false) ProjectGitFilePathsRequest request
	) {
		return ApiResponse.success(
			"GIT_FILE_UNSTAGE_SUCCESS",
			"파일 언스테이징에 성공했습니다.",
			projectGitService.unstageFiles(authentication.getName(), projectId, request == null ? null : request.filePaths())
		);
	}

	@Operation(
		summary = "전체 파일 스테이징",
		description = "전체 변경 파일을 Git staging area에 추가합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.GIT_REPOSITORY_PATH_NOT_FOUND,
		ErrorCode.GIT_REPOSITORY_NOT_FOUND,
		ErrorCode.GIT_STAGE_ALL_FAILED,
		ErrorCode.GIT_COMMAND_EXECUTION_FAILED
	})
	@PostMapping("/stage-all")
	public ApiResponse<ProjectGitChangeResponse> stageAll(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"GIT_STAGE_ALL_SUCCESS",
			"전체 파일 스테이징에 성공했습니다.",
			projectGitService.stageAll(authentication.getName(), projectId)
		);
	}

	@Operation(
		summary = "전체 파일 언스테이징",
		description = "전체 staged 파일을 Git staging area에서 제거합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.GIT_REPOSITORY_PATH_NOT_FOUND,
		ErrorCode.GIT_REPOSITORY_NOT_FOUND,
		ErrorCode.GIT_UNSTAGE_ALL_FAILED,
		ErrorCode.GIT_COMMAND_EXECUTION_FAILED
	})
	@PostMapping("/unstage-all")
	public ApiResponse<ProjectGitChangeResponse> unstageAll(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"GIT_UNSTAGE_ALL_SUCCESS",
			"전체 파일 언스테이징에 성공했습니다.",
			projectGitService.unstageAll(authentication.getName(), projectId)
		);
	}
}
