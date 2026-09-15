package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.request.ActiveSpringProfileUpdateRequest;
import com.weai.server.domain.project.request.ProjectEnvironmentVariableCreateRequest;
import com.weai.server.domain.project.request.ProjectEnvironmentVariableUpdateRequest;
import com.weai.server.domain.project.response.ActiveSpringProfileUpdateResponse;
import com.weai.server.domain.project.response.ProjectEnvironmentVariableDeleteResponse;
import com.weai.server.domain.project.response.ProjectEnvironmentVariableListResponse;
import com.weai.server.domain.project.response.ProjectEnvironmentVariableMutationResponse;
import com.weai.server.domain.project.response.ProfileRunCommandListResponse;
import com.weai.server.domain.project.response.RuntimeEnvironmentResponse;
import com.weai.server.domain.project.response.SpringProfileListResponse;
import com.weai.server.domain.project.service.ProjectEnvironmentService;
import com.weai.server.domain.project.service.ProjectEnvironmentVariableService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Environment", description = "프로젝트 실행 환경 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/environment")
public class ProjectEnvironmentController {

	private final ProjectRuntimeQueryService projectRuntimeQueryService;
	private final ProjectEnvironmentService projectEnvironmentService;
	private final ProjectEnvironmentVariableService projectEnvironmentVariableService;

	@Operation(summary = "환경변수 목록 조회", description = "프로젝트에 등록된 환경변수 목록을 조회합니다. 민감값은 마스킹되어 반환됩니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INVALID_SPRING_PROFILE,
		ErrorCode.ENVIRONMENT_VARIABLE_DECRYPT_FAILED
	})
	@GetMapping("/variables")
	public ApiResponse<ProjectEnvironmentVariableListResponse> getEnvironmentVariables(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "Spring profile(local/dev/test/prod)") @RequestParam(required = false) String profile,
		@Parameter(description = "사용 여부") @RequestParam(required = false) Boolean enabled
	) {
		return ApiResponse.success(
			"ENVIRONMENT_VARIABLE_LIST_SUCCESS", "환경변수 목록 조회에 성공했습니다.",
			projectEnvironmentVariableService.getEnvironmentVariablesForView(authentication.getName(), projectId, profile, enabled)
		);
	}

	@Operation(summary = "환경변수 추가", description = "프로젝트 실행에 사용할 환경변수를 추가합니다. 프로젝트 LEADER만 요청할 수 있습니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.PROJECT_LEADER_ONLY,
		ErrorCode.ENVIRONMENT_VARIABLE_KEY_REQUIRED, ErrorCode.ENVIRONMENT_VARIABLE_VALUE_REQUIRED,
		ErrorCode.SPRING_PROFILE_REQUIRED, ErrorCode.INVALID_ENVIRONMENT_VARIABLE_KEY,
		ErrorCode.INVALID_SPRING_PROFILE, ErrorCode.ENVIRONMENT_VARIABLE_ALREADY_EXISTS,
		ErrorCode.ENVIRONMENT_VARIABLE_ENCRYPT_FAILED
	})
	@PostMapping("/variables")
	public ApiResponse<ProjectEnvironmentVariableMutationResponse> createEnvironmentVariable(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@RequestBody(required = false) ProjectEnvironmentVariableCreateRequest request
	) {
		return ApiResponse.success(
			"ENVIRONMENT_VARIABLE_CREATE_SUCCESS", "환경변수가 추가되었습니다.",
			projectEnvironmentVariableService.createEnvironmentVariable(authentication.getName(), projectId, request)
		);
	}

	@Operation(summary = "환경변수 수정", description = "프로젝트 환경변수 설정을 부분 수정합니다. 키는 변경할 수 없으며 프로젝트 LEADER만 요청할 수 있습니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.PROJECT_LEADER_ONLY,
		ErrorCode.ENVIRONMENT_VARIABLE_NOT_FOUND, ErrorCode.ENVIRONMENT_VARIABLE_VALUE_REQUIRED,
		ErrorCode.SPRING_PROFILE_REQUIRED, ErrorCode.INVALID_SPRING_PROFILE,
		ErrorCode.ENVIRONMENT_VARIABLE_ALREADY_EXISTS, ErrorCode.ENVIRONMENT_VARIABLE_ENCRYPT_FAILED
	})
	@PatchMapping("/variables/{environmentVariableId}")
	public ApiResponse<ProjectEnvironmentVariableMutationResponse> updateEnvironmentVariable(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "환경변수 ID") @PathVariable Long environmentVariableId,
		@RequestBody(required = false) ProjectEnvironmentVariableUpdateRequest request
	) {
		return ApiResponse.success(
			"ENVIRONMENT_VARIABLE_UPDATE_SUCCESS", "환경변수가 수정되었습니다.",
			projectEnvironmentVariableService.updateEnvironmentVariable(authentication.getName(), projectId, environmentVariableId, request)
		);
	}

	@Operation(summary = "환경변수 삭제", description = "프로젝트 환경변수를 삭제합니다. 프로젝트 LEADER만 요청할 수 있습니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.PROJECT_LEADER_ONLY, ErrorCode.ENVIRONMENT_VARIABLE_NOT_FOUND
	})
	@DeleteMapping("/variables/{environmentVariableId}")
	public ApiResponse<ProjectEnvironmentVariableDeleteResponse> deleteEnvironmentVariable(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "환경변수 ID") @PathVariable Long environmentVariableId
	) {
		return ApiResponse.success(
			"ENVIRONMENT_VARIABLE_DELETE_SUCCESS", "환경변수가 삭제되었습니다.",
			projectEnvironmentVariableService.deleteEnvironmentVariable(authentication.getName(), projectId, environmentVariableId)
		);
	}

	@Operation(
		summary = "런타임 환경 정보 조회",
		description = "현재 서버의 런타임 환경 정보를 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.RUNTIME_ENVIRONMENT_READ_FAILED
	})
	@GetMapping("/runtime")
	public ApiResponse<RuntimeEnvironmentResponse> getRuntimeEnvironment(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId
	) {
		return ApiResponse.success(
			"RUNTIME_ENVIRONMENT_SUCCESS",
			"런타임 환경 정보 조회에 성공했습니다.",
			projectEnvironmentService.getRuntimeEnvironment(authentication.getName(), projectId)
		);
	}

	@Operation(
		summary = "Spring Profile 목록 조회",
		description = "프로젝트에서 사용할 수 있는 Spring Profile 목록을 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/profiles")
	public ApiResponse<SpringProfileListResponse> getSpringProfiles(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId
	) {
		return ApiResponse.success(
			"SPRING_PROFILE_LIST_SUCCESS",
			"Spring Profile 목록 조회에 성공했습니다.",
			projectEnvironmentService.getSpringProfiles(authentication.getName(), projectId)
		);
	}

	@Operation(
		summary = "Active Profile 변경",
		description = "프로젝트의 Active Profile 설정값을 변경합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.PROJECT_LEADER_ONLY,
		ErrorCode.SPRING_PROFILE_REQUIRED,
		ErrorCode.INVALID_SPRING_PROFILE,
		ErrorCode.ACTIVE_PROFILE_UPDATE_FAILED
	})
	@PatchMapping("/profiles/active")
	public ApiResponse<ActiveSpringProfileUpdateResponse> updateActiveProfile(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@RequestBody(required = false) ActiveSpringProfileUpdateRequest request
	) {
		return ApiResponse.success(
			"ACTIVE_SPRING_PROFILE_UPDATE_SUCCESS",
			"Active Profile이 변경되었습니다.",
			projectEnvironmentService.updateActiveProfile(authentication.getName(), projectId, request)
		);
	}

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
