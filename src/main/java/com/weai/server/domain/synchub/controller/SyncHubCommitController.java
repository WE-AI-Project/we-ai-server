package com.weai.server.domain.synchub.controller;

import com.weai.server.domain.project.response.ProjectCommitDetailResponse;
import com.weai.server.domain.project.response.ProjectCommitFileDiffResponse;
import com.weai.server.domain.project.response.ProjectCommitFileListResponse;
import com.weai.server.domain.project.response.ProjectCommitListResponse;
import com.weai.server.domain.synchub.service.SyncHubHistoryService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "SyncHub 커밋", description = "SyncHub 저장소 커밋/변경 파일 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/commits")
public class SyncHubCommitController {

	private final SyncHubHistoryService syncHubHistoryService;

	@Operation(
		summary = "프로젝트 커밋 목록 조회",
		description = "프로젝트 SyncHub 저장소 기본 브랜치의 최근 커밋 목록을 조회합니다. 저장소가 비어 있으면 빈 목록을 반환합니다. repositoryType은 호환용으로만 받습니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED
	})
	@GetMapping
	public ApiResponse<ProjectCommitListResponse> getProjectCommits(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(defaultValue = "BACKEND") String repositoryType,
		@RequestParam(defaultValue = "20") Integer limit
	) {
		return ApiResponse.success(
			"PROJECT_COMMIT_LIST_SUCCESS",
			"프로젝트 커밋 목록 조회에 성공했습니다.",
			syncHubHistoryService.getProjectCommits(authentication.getName(), projectId, repositoryType, limit)
		);
	}

	@Operation(
		summary = "프로젝트 저장소별 커밋 필터 조회",
		description = "커밋 목록 조회와 동일합니다(프로젝트당 SyncHub 저장소는 하나이며 repositoryType은 호환용으로만 받습니다)."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED
	})
	@GetMapping("/filter")
	public ApiResponse<ProjectCommitListResponse> filterProjectCommits(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(defaultValue = "BACKEND") String repositoryType,
		@RequestParam(defaultValue = "20") Integer limit
	) {
		return ApiResponse.success(
			"PROJECT_COMMIT_FILTER_SUCCESS",
			"프로젝트 저장소별 커밋 필터 조회에 성공했습니다.",
			syncHubHistoryService.getProjectCommits(authentication.getName(), projectId, repositoryType, limit)
		);
	}

	@Operation(
		summary = "프로젝트 커밋 상세 조회",
		description = "선택한 커밋의 메타데이터와 변경 통계를 상세 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED,
		ErrorCode.PROJECT_COMMIT_NOT_FOUND
	})
	@GetMapping("/{commitHash}")
	public ApiResponse<ProjectCommitDetailResponse> getProjectCommitDetail(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable String commitHash,
		@RequestParam(defaultValue = "BACKEND") String repositoryType
	) {
		return ApiResponse.success(
			"PROJECT_COMMIT_DETAIL_SUCCESS",
			"프로젝트 커밋 상세 조회에 성공했습니다.",
			syncHubHistoryService.getProjectCommitDetail(authentication.getName(), projectId, repositoryType, commitHash)
		);
	}

	@Operation(
		summary = "프로젝트 커밋 변경 파일 목록 조회",
		description = "선택한 커밋에서 변경된 파일 목록과 파일별 추가/삭제 라인 수를 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED,
		ErrorCode.PROJECT_COMMIT_NOT_FOUND
	})
	@GetMapping("/{commitHash}/files")
	public ApiResponse<ProjectCommitFileListResponse> getProjectCommitFiles(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable String commitHash,
		@RequestParam(defaultValue = "BACKEND") String repositoryType
	) {
		return ApiResponse.success(
			"PROJECT_COMMIT_FILE_LIST_SUCCESS",
			"프로젝트 커밋 변경 파일 목록 조회에 성공했습니다.",
			syncHubHistoryService.getProjectCommitFiles(authentication.getName(), projectId, repositoryType, commitHash)
		);
	}

	@Operation(
		summary = "프로젝트 커밋 파일 Diff 조회",
		description = "선택한 커밋의 특정 파일에 대한 unified diff 원문을 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED,
		ErrorCode.PROJECT_COMMIT_NOT_FOUND,
		ErrorCode.PROJECT_COMMIT_FILE_NOT_FOUND
	})
	@GetMapping("/{commitHash}/diff")
	public ApiResponse<ProjectCommitFileDiffResponse> getProjectCommitFileDiff(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable String commitHash,
		@RequestParam(defaultValue = "BACKEND") String repositoryType,
		@RequestParam String filePath
	) {
		return ApiResponse.success(
			"PROJECT_COMMIT_FILE_DIFF_SUCCESS",
			"프로젝트 커밋 파일 Diff 조회에 성공했습니다.",
			syncHubHistoryService.getProjectCommitFileDiff(authentication.getName(), projectId, repositoryType, commitHash, filePath)
		);
	}
}
