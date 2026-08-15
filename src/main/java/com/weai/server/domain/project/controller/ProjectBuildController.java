package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.response.BuildTaskListResponse;
import com.weai.server.domain.project.service.ProjectRuntimeQueryService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Build", description = "프로젝트 빌드 태스크 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/build")
public class ProjectBuildController {

	private final ProjectRuntimeQueryService projectRuntimeQueryService;

	@Operation(
		summary = "빌드 태스크 목록 조회",
		description = "프로젝트에서 실행 가능한 빌드 태스크 목록을 조회합니다. 실제 빌드 명령은 실행하지 않습니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/tasks")
	public ApiResponse<BuildTaskListResponse> getBuildTasks(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId
	) {
		return ApiResponse.success(
			"BUILD_TASK_LIST_SUCCESS",
			"빌드 태스크 목록 조회에 성공했습니다.",
			projectRuntimeQueryService.getBuildTasks(authentication.getName(), projectId)
		);
	}
}
