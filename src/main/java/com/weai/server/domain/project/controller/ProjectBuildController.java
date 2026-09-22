package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.request.ExecuteBuildTaskRequest;
import com.weai.server.domain.project.request.BuildTaskRunRequest;
import com.weai.server.domain.project.response.BuildRunHistoryResponse;
import com.weai.server.domain.project.response.BuildRunResultResponse;
import com.weai.server.domain.project.response.BuildTaskExecutionResponse;
import com.weai.server.domain.project.response.BuildTaskListResponse;
import com.weai.server.domain.project.response.BuildTaskRunResponse;
import com.weai.server.domain.project.service.ProjectBuildExecutionService;
import com.weai.server.domain.project.service.ProjectBuildRunService;
import com.weai.server.domain.project.service.ProjectRuntimeQueryService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Build", description = "프로젝트 빌드 태스크 조회 및 실행 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProjectBuildController {

	private final ProjectRuntimeQueryService projectRuntimeQueryService;
	private final ProjectBuildExecutionService projectBuildExecutionService;
	private final ProjectBuildRunService projectBuildRunService;

	@Operation(
		summary = "프로젝트 빌드 태스크 목록 조회",
		description = "프로젝트에서 실행 가능한 빌드 태스크 목록을 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/projects/{projectId}/build/tasks")
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

	@Operation(
		summary = "프로젝트 빌드 태스크 실제 실행",
		description = "프로젝트 작업 디렉터리에서 지정된 Gradle/Maven 태스크(build, test, clean 등)를 실제로 실행하고 전체 출력 로그를 반환합니다. 프로젝트 리더만 실행할 수 있습니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.PROJECT_LEADER_ONLY,
		ErrorCode.BUILD_LOCAL_PATH_NOT_FOUND
	})
	@PostMapping("/projects/{projectId}/build/execute")
	public ApiResponse<BuildTaskExecutionResponse> executeBuildTask(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Valid @RequestBody ExecuteBuildTaskRequest request
	) {
		return ApiResponse.success(
			"BUILD_TASK_EXECUTION_SUCCESS",
			"빌드 태스크가 성공적으로 실행되었습니다.",
			projectBuildExecutionService.executeTask(authentication.getName(), projectId, request.taskName())
		);
	}

	@Operation(
		summary = "빌드 태스크 실행",
		description = "프로젝트의 허용된 Gradle 빌드 태스크를 비동기로 실행합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.PROJECT_LEADER_ONLY,
		ErrorCode.PROJECT_LOCAL_PATH_NOT_FOUND,
		ErrorCode.GRADLE_WRAPPER_NOT_FOUND,
		ErrorCode.BUILD_TASK_NAME_REQUIRED,
		ErrorCode.INVALID_BUILD_TASK_NAME,
		ErrorCode.INVALID_SPRING_PROFILE,
		ErrorCode.BUILD_ALREADY_RUNNING,
		ErrorCode.BUILD_TASK_RUN_FAILED
	})
	@PostMapping("/projects/{projectId}/build/tasks/run")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ApiResponse<BuildTaskRunResponse> runBuildTask(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@RequestBody(required = false) BuildTaskRunRequest request
	) {
		return ApiResponse.success(
			"BUILD_TASK_RUN_SUCCESS",
			"빌드 태스크 실행이 시작되었습니다.",
			projectBuildRunService.runBuildTask(authentication.getName(), projectId, request)
		);
	}

	@Operation(
		summary = "빌드 실행 결과 조회",
		description = "특정 빌드 실행 건의 결과를 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.BUILD_RUN_NOT_FOUND
	})
	@GetMapping("/projects/{projectId}/build/runs/{buildRunId}")
	public ApiResponse<BuildRunResultResponse> getBuildRunResult(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "빌드 실행 ID") @PathVariable Long buildRunId
	) {
		return ApiResponse.success(
			"BUILD_RUN_RESULT_SUCCESS",
			"빌드 실행 결과 조회에 성공했습니다.",
			projectBuildRunService.getBuildRunResult(authentication.getName(), projectId, buildRunId)
		);
	}

	@Operation(
		summary = "빌드 히스토리 조회",
		description = "프로젝트의 빌드 실행 히스토리를 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.INVALID_BUILD_RUN_STATUS,
		ErrorCode.INVALID_BUILD_TASK_NAME,
		ErrorCode.INVALID_SPRING_PROFILE
	})
	@GetMapping("/projects/{projectId}/build/runs")
	public ApiResponse<BuildRunHistoryResponse> getBuildRunHistory(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@RequestParam(defaultValue = "0") Integer page,
		@RequestParam(defaultValue = "20") Integer size,
		@RequestParam(required = false) String status,
		@RequestParam(required = false) String taskName,
		@RequestParam(required = false) String profile
	) {
		return ApiResponse.success(
			"BUILD_RUN_HISTORY_SUCCESS",
			"빌드 히스토리 조회에 성공했습니다.",
			projectBuildRunService.getBuildRunHistory(authentication.getName(), projectId, page, size, status, taskName, profile)
		);
	}

	@Operation(
		summary = "시스템 빌드 태스크 목록 조회",
		description = "서버 루트 환경에서 실행 가능한 빌드 태스크 목록을 조회합니다. ADMIN 전용입니다."
	)
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/build/tasks")
	public ApiResponse<BuildTaskListResponse> getSystemBuildTasks() {
		return ApiResponse.success(
			"BUILD_TASK_LIST_SUCCESS",
			"빌드 태스크 목록 조회에 성공했습니다.",
			new BuildTaskListResponse(
				0L,
				"GRADLE",
				java.util.List.of(
					new BuildTaskListResponse.BuildTaskResponse("bootRun", "Spring Boot 실행", "Spring Boot 애플리케이션을 실행합니다.", "./gradlew.bat bootRun", "RUN", false, true),
					new BuildTaskListResponse.BuildTaskResponse("build", "Build", "프로젝트 전체를 빌드합니다.", "./gradlew.bat build", "BUILD", false, true),
					new BuildTaskListResponse.BuildTaskResponse("test", "Test", "단위/통합 테스트를 실행합니다.", "./gradlew.bat test", "TEST", false, true),
					new BuildTaskListResponse.BuildTaskResponse("clean", "Clean", "빌드 결과물 디렉터리를 정리합니다.", "./gradlew.bat clean", "BUILD", true, true),
					new BuildTaskListResponse.BuildTaskResponse("dependencies", "Dependencies", "의존성 트리를 출력합니다.", "./gradlew.bat dependencies", "INFO", false, true),
					new BuildTaskListResponse.BuildTaskResponse("bootJar", "Boot Jar", "실행 가능한 jar 파일을 생성합니다.", "./gradlew.bat bootJar", "BUILD", false, true),
					new BuildTaskListResponse.BuildTaskResponse("check", "Check", "코드 스타일 및 검증 태스크를 실행합니다.", "./gradlew.bat check", "TEST", false, true)
				)
			)
		);
	}

	@Operation(
		summary = "시스템 빌드 태스크 실제 실행",
		description = "서버 환경(SynAIpse 서버 자신의 소스 트리)에서 지정된 Gradle 태스크를 실제로 실행하고 출력 로그를 반환합니다. ADMIN 전용입니다."
	)
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/build/execute")
	public ApiResponse<BuildTaskExecutionResponse> executeSystemBuildTask(
		@Valid @RequestBody ExecuteBuildTaskRequest request
	) {
		return ApiResponse.success(
			"BUILD_TASK_EXECUTION_SUCCESS",
			"빌드 태스크가 성공적으로 실행되었습니다.",
			projectBuildExecutionService.executeSystemTask(request.taskName())
		);
	}
}
