package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import com.weai.server.domain.project.request.ProjectCreateRequest;
import com.weai.server.domain.project.request.ProjectJoinRequest;
import com.weai.server.domain.project.request.ProjectMemberDepartmentUpdateRequest;
import com.weai.server.domain.project.request.ProjectMemberRoleUpdateRequest;
import com.weai.server.domain.project.request.ProjectScheduleCreateRequest;
import com.weai.server.domain.project.request.ProjectScheduleStatusUpdateRequest;
import com.weai.server.domain.project.request.ProjectScheduleUpdateRequest;
import com.weai.server.domain.project.request.ProjectStackDetectRequest;
import com.weai.server.domain.project.request.ProjectTechStackCreateRequest;
import com.weai.server.domain.project.request.ProjectTechStackUpdateRequest;
import com.weai.server.domain.project.request.ProjectUpdateRequest;
import com.weai.server.domain.project.response.MyProjectResponse;
import com.weai.server.domain.project.response.ProjectCreateResponse;
import com.weai.server.domain.project.response.ProjectDashboardResponse;
import com.weai.server.domain.project.response.ProjectDetailResponse;
import com.weai.server.domain.project.response.ProjectJoinResponse;
import com.weai.server.domain.project.response.ProjectMemberDetailResponse;
import com.weai.server.domain.project.response.ProjectMemberListResponse;
import com.weai.server.domain.project.response.ProjectMemberUpdateResponse;
import com.weai.server.domain.project.response.ProjectScheduleCreateResponse;
import com.weai.server.domain.project.response.ProjectScheduleDeleteResponse;
import com.weai.server.domain.project.response.ProjectScheduleDetailResponse;
import com.weai.server.domain.project.response.ProjectScheduleListResponse;
import com.weai.server.domain.project.response.ProjectStackDetectResponse;
import com.weai.server.domain.project.response.ProjectTechStackDeleteResponse;
import com.weai.server.domain.project.response.ProjectTechStackListResponse;
import com.weai.server.domain.project.response.ProjectTechStackResponse;
import com.weai.server.domain.project.response.ProjectUpdateResponse;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.project.service.ProjectStackDetectionService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "프로젝트", description = "프로젝트 생성, 참여, 멤버/일정/기술 스택 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

	private final ProjectService projectService;
	private final ProjectStackDetectionService projectStackDetectionService;

	@Operation(summary = "Detect project technology stack from a local path")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT})
	@PostMapping("/detect-stack")
	public ApiResponse<ProjectStackDetectResponse> detectStack(@Valid @RequestBody ProjectStackDetectRequest request) {
		return ApiResponse.success(
			"PROJECT_STACK_DETECT_SUCCESS",
			"Project technology stack detection completed.",
			projectStackDetectionService.detect(request.localPath())
		);
	}

	@Operation(
		summary = "프로젝트 생성",
		description = "프로젝트를 생성하고 생성자를 리더로 등록합니다. 설명과 마감일은 선택값입니다."
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
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "프로젝트 생성 요청",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "프로젝트 생성 예시",
					value = """
						{
						  "projectName": "Schedule API Test Project",
						  "description": "Swagger test project",
						  "localPath": "C:\\\\WE_AI\\\\schedule-api-test",
						  "department": "BACKEND",
						  "deadlineDate": "2026-06-30"
						}
						"""
				)
			)
		)
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

	@Operation(summary = "참여 코드로 프로젝트 참여", description = "8자리 참여 코드를 사용해 활성 프로젝트에 참여합니다.")
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

	@Operation(summary = "프로젝트 대시보드 조회", description = "프로젝트 요약, 진행률, 부서별 진행 현황을 조회합니다.")
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
			"프로젝트 대시보드 조회에 성공했습니다.",
			projectService.getProjectDashboard(authentication.getName(), projectId)
		);
	}

	@Operation(summary = "프로젝트 상세 조회", description = "로그인 사용자가 접근 가능한 프로젝트의 상세 정보를 조회합니다.")
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
			"프로젝트 상세 조회에 성공했습니다.",
			projectService.getProjectDetail(authentication.getName(), projectId)
		);
	}

	@Operation(summary = "프로젝트 정보 수정", description = "프로젝트 기본 정보를 부분 수정합니다. 프로젝트 리더만 수정할 수 있습니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NAME_REQUIRED,
		ErrorCode.PROJECT_NAME_TOO_LONG,
		ErrorCode.INVALID_PROJECT_DATE,
		ErrorCode.INVALID_PROJECT_STATUS,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.PROJECT_LEADER_ONLY
	})
	@PatchMapping("/{projectId}")
	public ApiResponse<ProjectUpdateResponse> updateProject(
		Authentication authentication,
		@PathVariable Long projectId,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "프로젝트 정보 수정 요청",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "프로젝트 정보 수정 예시",
					value = """
						{
						  "projectName": "Synaipse Project",
						  "description": "AI 기반 개발 협업 플랫폼",
						  "repositoryUrl": "https://github.com/example/synaipse",
						  "localPath": "D:\\Synaipse",
						  "startDate": "2026-05-01",
						  "targetDate": "2026-06-30",
						  "status": "ACTIVE"
						}
						"""
				)
			)
		)
		@Valid @RequestBody ProjectUpdateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_UPDATE_SUCCESS",
			"프로젝트 정보가 수정되었습니다.",
			projectService.updateProject(authentication.getName(), projectId, request)
		);
	}

	@Operation(summary = "프로젝트 멤버 목록 조회", description = "프로젝트의 ACTIVE 멤버 목록을 조회합니다.")
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

	@Operation(summary = "프로젝트 멤버 상세 조회", description = "프로젝트의 특정 멤버 상세 정보를 조회합니다. 프로젝트의 ACTIVE 멤버라면 누구나 조회할 수 있습니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.PROJECT_MEMBER_NOT_FOUND
	})
	@GetMapping("/{projectId}/members/{memberId}")
	public ApiResponse<ProjectMemberDetailResponse> getProjectMemberDetail(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long memberId
	) {
		return ApiResponse.success(
			"PROJECT_MEMBER_DETAIL_SUCCESS",
			"프로젝트 멤버 상세 조회에 성공했습니다.",
			projectService.getProjectMemberDetail(authentication.getName(), projectId, memberId)
		);
	}

	@Operation(summary = "프로젝트 멤버 역할 변경", description = "프로젝트 멤버의 역할을 변경합니다. 프로젝트 리더만 변경할 수 있습니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.PROJECT_LEADER_ONLY,
		ErrorCode.PROJECT_MEMBER_NOT_FOUND,
		ErrorCode.PROJECT_MEMBER_ROLE_REQUIRED,
		ErrorCode.INVALID_PROJECT_MEMBER_ROLE,
		ErrorCode.PROJECT_MEMBER_NOT_ACTIVE,
		ErrorCode.CANNOT_CHANGE_OWN_LEADER_ROLE,
		ErrorCode.PROJECT_LEADER_REQUIRED
	})
	@PatchMapping("/{projectId}/members/{memberId}/role")
	public ApiResponse<ProjectMemberUpdateResponse> updateProjectMemberRole(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long memberId,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "프로젝트 멤버 역할 변경 요청",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "프로젝트 멤버 역할 변경 예시",
					value = """
						{
						  "role": "MEMBER"
						}
						"""
				)
			)
		)
		@Valid @RequestBody ProjectMemberRoleUpdateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_MEMBER_ROLE_UPDATE_SUCCESS",
			"프로젝트 멤버 역할이 변경되었습니다.",
			projectService.updateProjectMemberRole(authentication.getName(), projectId, memberId, request)
		);
	}

	@Operation(summary = "프로젝트 멤버 부서 변경", description = "프로젝트 멤버의 부서를 변경합니다. 프로젝트 리더만 변경할 수 있습니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.PROJECT_LEADER_ONLY,
		ErrorCode.PROJECT_MEMBER_NOT_FOUND,
		ErrorCode.PROJECT_MEMBER_DEPARTMENT_REQUIRED,
		ErrorCode.INVALID_PROJECT_MEMBER_DEPARTMENT,
		ErrorCode.PROJECT_MEMBER_NOT_ACTIVE
	})
	@PatchMapping("/{projectId}/members/{memberId}/department")
	public ApiResponse<ProjectMemberUpdateResponse> updateProjectMemberDepartment(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long memberId,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "프로젝트 멤버 부서 변경 요청",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "프로젝트 멤버 부서 변경 예시",
					value = """
						{
						  "department": "FRONTEND"
						}
						"""
				)
			)
		)
		@Valid @RequestBody ProjectMemberDepartmentUpdateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_MEMBER_DEPARTMENT_UPDATE_SUCCESS",
			"프로젝트 멤버 부서가 변경되었습니다.",
			projectService.updateProjectMemberDepartment(authentication.getName(), projectId, memberId, request)
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

	@Operation(summary = "프로젝트 기술 스택 추가", description = "프로젝트에 새로운 기술 스택을 추가합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.TECH_STACK_NAME_REQUIRED,
		ErrorCode.TECH_STACK_CATEGORY_REQUIRED,
		ErrorCode.TECH_STACK_ALREADY_EXISTS
	})
	@PostMapping("/{projectId}/tech-stacks")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ProjectTechStackResponse> createProjectTechStack(
		Authentication authentication,
		@PathVariable Long projectId,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "프로젝트 기술 스택 추가 요청",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "프로젝트 기술 스택 추가 예시",
					value = """
						{
						  "name": "Spring Boot",
						  "version": "3.2.5",
						  "category": "BACKEND",
						  "isRequired": true
						}
						"""
				)
			)
		)
		@Valid @RequestBody ProjectTechStackCreateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_TECH_STACK_CREATE_SUCCESS",
			"프로젝트 기술 스택 추가에 성공했습니다.",
			projectService.createProjectTechStack(authentication.getName(), projectId, request)
		);
	}

	@Operation(summary = "프로젝트 기술 스택 수정", description = "프로젝트에 등록된 기술 스택 정보를 부분 수정합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.TECH_STACK_NOT_FOUND,
		ErrorCode.TECH_STACK_NAME_REQUIRED,
		ErrorCode.TECH_STACK_ALREADY_EXISTS
	})
	@PatchMapping("/{projectId}/tech-stacks/{techStackId}")
	public ApiResponse<ProjectTechStackResponse> updateProjectTechStack(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long techStackId,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "프로젝트 기술 스택 수정 요청",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "프로젝트 기술 스택 수정 예시",
					value = """
						{
						  "name": "Spring Boot",
						  "version": "3.3.0",
						  "category": "BACKEND",
						  "isRequired": true
						}
						"""
				)
			)
		)
		@Valid @RequestBody ProjectTechStackUpdateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_TECH_STACK_UPDATE_SUCCESS",
			"프로젝트 기술 스택 수정에 성공했습니다.",
			projectService.updateProjectTechStack(authentication.getName(), projectId, techStackId, request)
		);
	}

	@Operation(summary = "프로젝트 기술 스택 삭제", description = "프로젝트에 등록된 기술 스택을 삭제합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.TECH_STACK_NOT_FOUND
	})
	@DeleteMapping("/{projectId}/tech-stacks/{techStackId}")
	public ApiResponse<ProjectTechStackDeleteResponse> deleteProjectTechStack(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long techStackId
	) {
		return ApiResponse.success(
			"PROJECT_TECH_STACK_DELETE_SUCCESS",
			"프로젝트 기술 스택 삭제에 성공했습니다.",
			projectService.deleteProjectTechStack(authentication.getName(), projectId, techStackId)
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

	@Operation(summary = "프로젝트 일정 필터 조회", description = "프로젝트 일정을 부서, 상태, 기간 조건으로 필터링하여 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/schedules/filter")
	public ApiResponse<ProjectScheduleListResponse> getFilteredProjectSchedules(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(required = false) ProjectDepartment department,
		@RequestParam(required = false) ProjectScheduleStatus status,
		@RequestParam(required = false) LocalDate startDate,
		@RequestParam(required = false) LocalDate endDate
	) {
		return ApiResponse.success(
			"PROJECT_SCHEDULE_FILTER_SUCCESS",
			"프로젝트 일정 필터 조회에 성공했습니다.",
			projectService.getProjectSchedules(authentication.getName(), projectId, department, status, startDate, endDate)
		);
	}

	@Operation(summary = "프로젝트 일정 생성", description = "프로젝트에 새로운 일정을 생성합니다.")
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
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "프로젝트 일정 생성 요청",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "프로젝트 일정 생성 예시",
					value = """
						{
						  "title": "프로젝트 일정 상세 API 구현",
						  "description": "일정 상세 조회 API 개발",
						  "department": "BACKEND",
						  "startDate": "2026-05-24",
						  "endDate": "2026-05-24",
						  "priority": "HIGH",
						  "status": "TODO"
						}
						"""
				)
			)
		)
		@Valid @RequestBody ProjectScheduleCreateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_SCHEDULE_CREATE_SUCCESS",
			"프로젝트 일정이 생성되었습니다.",
			projectService.createProjectSchedule(authentication.getName(), projectId, request)
		);
	}

	@Operation(summary = "프로젝트 일정 상세 조회", description = "프로젝트에 속한 일정의 상세 정보를 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SCHEDULE_NOT_FOUND
	})
	@GetMapping("/{projectId}/schedules/{scheduleId}")
	public ApiResponse<ProjectScheduleDetailResponse> getProjectScheduleDetail(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long scheduleId
	) {
		return ApiResponse.success(
			"PROJECT_SCHEDULE_DETAIL_SUCCESS",
			"프로젝트 일정 상세 조회에 성공했습니다.",
			projectService.getProjectScheduleDetail(authentication.getName(), projectId, scheduleId)
		);
	}

	@Operation(summary = "프로젝트 일정 수정", description = "프로젝트에 속한 일정 정보를 부분 수정합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SCHEDULE_NOT_FOUND,
		ErrorCode.SCHEDULE_TITLE_REQUIRED,
		ErrorCode.INVALID_SCHEDULE_DATE,
		ErrorCode.INVALID_SCHEDULE_STATUS,
		ErrorCode.ASSIGNEE_NOT_FOUND,
		ErrorCode.ASSIGNEE_NOT_PROJECT_MEMBER
	})
	@PatchMapping("/{projectId}/schedules/{scheduleId}")
	public ApiResponse<ProjectScheduleDetailResponse> updateProjectSchedule(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long scheduleId,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "프로젝트 일정 수정 요청",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "프로젝트 일정 수정 예시",
					value = """
						{
						  "title": "프로젝트 일정 수정 API 구현",
						  "description": "일정 수정 기능 개발",
						  "assigneeId": 7,
						  "department": "BACKEND",
						  "startDate": "2026-05-24",
						  "endDate": "2026-05-25",
						  "priority": "HIGH",
						  "status": "IN_PROGRESS"
						}
						"""
				)
			)
		)
		@Valid @RequestBody ProjectScheduleUpdateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_SCHEDULE_UPDATE_SUCCESS",
			"프로젝트 일정 수정에 성공했습니다.",
			projectService.updateProjectSchedule(authentication.getName(), projectId, scheduleId, request)
		);
	}

	@Operation(summary = "프로젝트 일정 상태 변경", description = "프로젝트 일정의 상태만 변경합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SCHEDULE_NOT_FOUND,
		ErrorCode.SCHEDULE_STATUS_REQUIRED,
		ErrorCode.INVALID_SCHEDULE_STATUS
	})
	@PatchMapping("/{projectId}/schedules/{scheduleId}/status")
	public ApiResponse<ProjectScheduleDetailResponse> updateProjectScheduleStatus(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long scheduleId,
		@RequestBody ProjectScheduleStatusUpdateRequest request
	) {
		return ApiResponse.success(
			"PROJECT_SCHEDULE_STATUS_UPDATE_SUCCESS",
			"프로젝트 일정 상태 변경에 성공했습니다.",
			projectService.updateProjectScheduleStatus(authentication.getName(), projectId, scheduleId, request)
		);
	}

	@Operation(summary = "프로젝트 일정 삭제", description = "프로젝트에 속한 일정을 삭제합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SCHEDULE_NOT_FOUND
	})
	@DeleteMapping("/{projectId}/schedules/{scheduleId}")
	public ApiResponse<ProjectScheduleDeleteResponse> deleteProjectSchedule(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long scheduleId
	) {
		return ApiResponse.success(
			"PROJECT_SCHEDULE_DELETE_SUCCESS",
			"프로젝트 일정 삭제에 성공했습니다.",
			projectService.deleteProjectSchedule(authentication.getName(), projectId, scheduleId)
		);
	}
}
