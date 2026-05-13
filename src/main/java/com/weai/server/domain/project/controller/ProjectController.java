package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.request.ProjectCreateRequest;
import com.weai.server.domain.project.request.ProjectJoinRequest;
import com.weai.server.domain.project.request.ProjectScheduleCreateRequest;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import com.weai.server.domain.project.response.MyProjectResponse;
import com.weai.server.domain.project.response.ProjectCreateResponse;
import com.weai.server.domain.project.response.ProjectDashboardResponse;
import com.weai.server.domain.project.response.ProjectDetailResponse;
import com.weai.server.domain.project.response.ProjectJoinResponse;
import com.weai.server.domain.project.response.ProjectMemberListResponse;
import com.weai.server.domain.project.response.ProjectScheduleCreateResponse;
import com.weai.server.domain.project.response.ProjectScheduleListResponse;
import com.weai.server.domain.project.response.ProjectTechStackListResponse;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "프로젝트", description = "로그인 이후 프로젝트 생성, 참여, 대시보드 진입에 사용하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

	private final ProjectService projectService;

	@Operation(
		summary = "프로젝트 생성",
		description = "프로젝트를 생성하고 생성자를 리더로 등록합니다. 저장 위치는 필수이며, 마감일은 선택값입니다. 마감일을 보내면 응답에서 오늘 기준 남은 일수를 함께 반환합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NAME_REQUIRED,
		ErrorCode.PROJECT_NAME_TOO_LONG,
		ErrorCode.PROJECT_PATH_REQUIRED,
		ErrorCode.INVALID_PROJECT_DATE,
		ErrorCode.PROJECT_CODE_GENERATION_FAILED,
		ErrorCode.PROJECT_CREATE_FAILED
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ProjectCreateResponse> createProject(
		Authentication authentication,
		@Valid @RequestBody ProjectCreateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_CREATE_SUCCESS",
			"프로젝트가 생성되었습니다.",
			projectService.createProject(authentication.getName(), request)
		);
	}

	@Operation(summary = "내 프로젝트 목록 조회", description = "현재 로그인한 사용자가 참여 중인 활성 프로젝트 목록을 조회합니다.")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED})
	@GetMapping("/my")
	public ApiResponse<List<MyProjectResponse>> getMyProjects(Authentication authentication) {
		List<MyProjectResponse> projects = projectService.getMyProjects(authentication.getName());
		String message = projects.isEmpty()
			? "참여 중인 프로젝트가 없습니다."
			: "내 프로젝트 목록 조회에 성공했습니다.";
		return ApiResponse.success("PROJECT_LIST_SUCCESS", message, projects);
	}

	@Operation(summary = "참여 코드로 프로젝트 참여", description = "8자리 참여 코드를 입력해 활성 프로젝트에 참여합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_CODE_REQUIRED,
		ErrorCode.INVALID_PROJECT_CODE_FORMAT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.ALREADY_JOINED_PROJECT,
		ErrorCode.PROJECT_JOIN_FAILED
	})
	@PostMapping("/join")
	public ApiResponse<ProjectJoinResponse> joinProject(
		Authentication authentication,
		@Valid @RequestBody ProjectJoinRequest request
	) {
		return ApiResponse.success(
			"PROJECT_JOIN_SUCCESS",
			"프로젝트 참여에 성공했습니다.",
			projectService.joinProject(authentication.getName(), request)
		);
	}

	@Operation(summary = "프로젝트 대시보드 요약 조회", description = "로그인 사용자가 참여 중인 프로젝트의 대시보드 요약 정보를 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/dashboard")
	public ApiResponse<ProjectDashboardResponse> getProjectDashboard(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"PROJECT_DASHBOARD_SUCCESS",
			"프로젝트 대시보드 요약 조회에 성공했습니다.",
			projectService.getProjectDashboard(authentication.getName(), projectId)
		);
	}

	@Operation(summary = "프로젝트 상세 정보 조회", description = "로그인 사용자가 참여 중인 프로젝트의 상세 정보를 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}")
	public ApiResponse<ProjectDetailResponse> getProjectDetail(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"PROJECT_DETAIL_SUCCESS",
			"프로젝트 상세 정보 조회에 성공했습니다.",
			projectService.getProjectDetail(authentication.getName(), projectId)
		);
	}

	@Operation(summary = "프로젝트 멤버 목록 조회", description = "프로젝트에 참여 중인 ACTIVE 멤버 목록을 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/members")
	public ApiResponse<ProjectMemberListResponse> getProjectMembers(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"PROJECT_MEMBER_LIST_SUCCESS",
			"프로젝트 멤버 목록 조회에 성공했습니다.",
			projectService.getProjectMembers(authentication.getName(), projectId)
		);
	}

	@Operation(summary = "프로젝트 기술 스택 조회", description = "프로젝트에 등록된 기술 스택 목록을 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/tech-stacks")
	public ApiResponse<ProjectTechStackListResponse> getProjectTechStacks(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"PROJECT_TECH_STACK_LIST_SUCCESS",
			"프로젝트 기술 스택 조회에 성공했습니다.",
			projectService.getProjectTechStacks(authentication.getName(), projectId)
		);
	}

	@Operation(summary = "프로젝트 일정 목록 조회", description = "프로젝트 일정 목록을 필터 조건과 함께 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/schedules")
	public ApiResponse<ProjectScheduleListResponse> getProjectSchedules(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(required = false) ProjectDepartment department,
		@RequestParam(required = false) ProjectScheduleStatus status,
		@RequestParam(required = false) LocalDate startDate,
		@RequestParam(required = false) LocalDate endDate
	) {
		return ApiResponse.success(
			"PROJECT_SCHEDULE_LIST_SUCCESS",
			"프로젝트 일정 목록 조회에 성공했습니다.",
			projectService.getProjectSchedules(authentication.getName(), projectId, department, status, startDate, endDate)
		);
	}

	@Operation(summary = "프로젝트 일정 생성", description = "프로젝트에 새 일정을 생성합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.ASSIGNEE_NOT_FOUND,
		ErrorCode.ASSIGNEE_NOT_PROJECT_MEMBER,
		ErrorCode.SCHEDULE_TITLE_REQUIRED,
		ErrorCode.INVALID_SCHEDULE_DATE,
		ErrorCode.SCHEDULE_CREATE_FAILED
	})
	@PostMapping("/{projectId}/schedules")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ProjectScheduleCreateResponse> createProjectSchedule(
		Authentication authentication,
		@PathVariable Long projectId,
		@Valid @RequestBody ProjectScheduleCreateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_SCHEDULE_CREATE_SUCCESS",
			"프로젝트 일정이 생성되었습니다.",
			projectService.createProjectSchedule(authentication.getName(), projectId, request)
		);
	}
}
