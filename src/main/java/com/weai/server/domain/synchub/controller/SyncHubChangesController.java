package com.weai.server.domain.synchub.controller;

import com.weai.server.domain.project.request.ProjectGitCommitConventionCheckRequest;
import com.weai.server.domain.project.response.ProjectGitBranchGraphResponse;
import com.weai.server.domain.project.response.ProjectGitCommitConventionCheckResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-side part of the Changes/Analytics tabs. Working-tree operations (changed files, stage,
 * commit, ...) are no longer served here - the desktop app runs them against the local checkout
 * and pushes to SyncHub; only the branch graph (read from SyncHub) and the pure-text commit
 * convention check remain.
 */
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "SyncHub 변경사항", description = "SyncHub 브랜치 그래프 및 커밋 컨벤션 검사 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/changes")
public class SyncHubChangesController {

	private final SyncHubHistoryService syncHubHistoryService;

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
			syncHubHistoryService.checkCommitConvention(authentication.getName(), projectId, request)
		);
	}

	@Operation(
		summary = "브랜치 그래프 조회",
		description = "프로젝트 SyncHub 저장소의 브랜치와 커밋 관계를 그래프 형태로 조회합니다. 저장소가 비어 있으면 빈 그래프를 반환합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.INVALID_GIT_BRANCH_NAME,
		ErrorCode.GIT_BRANCH_NOT_FOUND,
		ErrorCode.INVALID_GIT_GRAPH_LIMIT,
		ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED
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
			syncHubHistoryService.getBranchGraph(authentication.getName(), projectId, branch, maxCount, includeRemote)
		);
	}
}
