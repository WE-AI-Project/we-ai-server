package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.request.ProjectGitCommitRequest;
import com.weai.server.domain.project.request.ProjectGitCommitConventionCheckRequest;
import com.weai.server.domain.project.request.ProjectGitFilePathsRequest;
import com.weai.server.domain.project.response.ProjectGitBranchGraphResponse;
import com.weai.server.domain.project.response.ProjectChangedFileListResponse;
import com.weai.server.domain.project.response.ProjectGitChangeResponse;
import com.weai.server.domain.project.response.ProjectGitCommitConventionCheckResponse;
import com.weai.server.domain.project.response.ProjectGitCommitCreateResponse;
import com.weai.server.domain.project.response.ProjectGitFileDiffResponse;
import com.weai.server.domain.project.service.ProjectGitService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Git Changes", description = "프로젝트 Git 변경사항 스테이징 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/changes")
public class ProjectGitChangesController {

	private final ProjectGitService projectGitService;

	@Operation(
		summary = "변경 파일 목록 조회",
		description = "프로젝트 Git 저장소의 변경 파일 목록을 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.GIT_REPOSITORY_PATH_NOT_FOUND,
		ErrorCode.GIT_REPOSITORY_NOT_FOUND,
		ErrorCode.GIT_COMMAND_EXECUTION_FAILED
	})
	@GetMapping("/files")
	public ApiResponse<ProjectChangedFileListResponse> getChangedFiles(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"GIT_CHANGED_FILE_LIST_SUCCESS",
			"변경 파일 목록 조회에 성공했습니다.",
			projectGitService.getChangedFiles(authentication.getName(), projectId)
		);
	}

	@Operation(
		summary = "변경 파일 Diff 조회",
		description = "선택한 변경 파일의 Diff 내용을 조회합니다."
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
		ErrorCode.GIT_CHANGED_FILE_NOT_FOUND,
		ErrorCode.GIT_DIFF_FAILED,
		ErrorCode.GIT_COMMAND_EXECUTION_FAILED
	})
	@GetMapping("/diff")
	public ApiResponse<ProjectGitFileDiffResponse> getChangedFileDiff(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(required = false) String filePath,
		@RequestParam(defaultValue = "false") boolean staged
	) {
		return ApiResponse.success(
			"GIT_FILE_DIFF_SUCCESS",
			"변경 파일 Diff 조회에 성공했습니다.",
			projectGitService.getChangedFileDiff(authentication.getName(), projectId, filePath, staged)
		);
	}

	@Operation(
		summary = "커밋 생성",
		description = "현재 staged 파일들을 Git 커밋으로 생성합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.GIT_REPOSITORY_PATH_NOT_FOUND,
		ErrorCode.GIT_REPOSITORY_NOT_FOUND,
		ErrorCode.GIT_COMMIT_MESSAGE_REQUIRED,
		ErrorCode.GIT_COMMIT_MESSAGE_TOO_LONG,
		ErrorCode.GIT_COMMIT_DESCRIPTION_TOO_LONG,
		ErrorCode.GIT_NO_STAGED_FILES,
		ErrorCode.GIT_COMMIT_FAILED,
		ErrorCode.GIT_COMMAND_EXECUTION_FAILED
	})
	@PostMapping("/commit")
	public ApiResponse<ProjectGitCommitCreateResponse> createCommit(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestBody(required = false) ProjectGitCommitRequest request
	) {
		return ApiResponse.success(
			"GIT_COMMIT_CREATE_SUCCESS",
			"커밋 생성에 성공했습니다.",
			projectGitService.createCommit(authentication.getName(), projectId, request)
		);
	}

	@Operation(
		summary = "커밋 컨벤션 검사",
		description = "입력한 커밋 메시지가 프로젝트 커밋 컨벤션에 맞는지 검사합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.GIT_COMMIT_MESSAGE_REQUIRED,
		ErrorCode.GIT_COMMIT_MESSAGE_TOO_LONG,
		ErrorCode.GIT_COMMIT_DESCRIPTION_TOO_LONG
	})
	@PostMapping("/commit-convention/check")
	public ApiResponse<ProjectGitCommitConventionCheckResponse> checkCommitConvention(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestBody(required = false) ProjectGitCommitConventionCheckRequest request
	) {
		return ApiResponse.success(
			"GIT_COMMIT_CONVENTION_CHECK_SUCCESS",
			"커밋 컨벤션 검사에 성공했습니다.",
			projectGitService.checkCommitConvention(authentication.getName(), projectId, request)
		);
	}

	@Operation(
		summary = "브랜치 그래프 조회",
		description = "프로젝트 Git 저장소의 브랜치와 커밋 관계를 그래프 형태로 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.GIT_REPOSITORY_PATH_NOT_FOUND,
		ErrorCode.GIT_REPOSITORY_NOT_FOUND,
		ErrorCode.INVALID_GIT_BRANCH_NAME,
		ErrorCode.GIT_BRANCH_NOT_FOUND,
		ErrorCode.INVALID_GIT_GRAPH_LIMIT,
		ErrorCode.GIT_BRANCH_GRAPH_FAILED,
		ErrorCode.GIT_COMMAND_EXECUTION_FAILED
	})
	@GetMapping("/branches/graph")
	public ApiResponse<ProjectGitBranchGraphResponse> getBranchGraph(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(required = false) String branch,
		@RequestParam(required = false) Integer maxCount,
		@RequestParam(defaultValue = "true") boolean includeRemote
	) {
		return ApiResponse.success(
			"GIT_BRANCH_GRAPH_SUCCESS",
			"브랜치 그래프 조회에 성공했습니다.",
			projectGitService.getBranchGraph(authentication.getName(), projectId, branch, maxCount, includeRemote)
		);
	}

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
