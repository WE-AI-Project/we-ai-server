package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.response.ProfileRunCommandListResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Environment", description = "프로젝트 실행 환경 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/environment")
public class ProjectEnvironmentController {

	private final ProjectRuntimeQueryService projectRuntimeQueryService;

	@Operation(
		summary = "프로파일별 실행 명령 조회",
		description = "Spring profile별 실행 명령 문자열을 조회합니다. 실제 실행 명령은 수행하지 않습니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_SPRING_PROFILE,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/run-commands")
	public ApiResponse<ProfileRunCommandListResponse> getProfileRunCommands(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "Spring profile(local/dev/test/prod)") @RequestParam(required = false) String profile
	) {
		return ApiResponse.success(
			"PROFILE_RUN_COMMAND_LIST_SUCCESS",
			"프로파일별 실행 명령 조회에 성공했습니다.",
			projectRuntimeQueryService.getProfileRunCommands(authentication.getName(), projectId, profile)
		);
	}
}
