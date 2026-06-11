package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.response.ProjectDepartmentStatusResponse;
import com.weai.server.domain.project.response.ProjectMilestoneListResponse;
import com.weai.server.domain.project.response.ProjectProgressStatResponse;
import com.weai.server.domain.project.response.ProjectRecentActivityListResponse;
import com.weai.server.domain.project.service.ProjectService;
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
@Tag(name = "프로젝트 대시보드", description = "프로젝트 대시보드 요약 및 세부 카드 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectDashboardController {

	private final ProjectService projectService;

	@Operation(summary = "프로젝트 최근 활동 목록 조회", description = "프로젝트 대시보드의 최근 활동 카드에 필요한 활동 목록을 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/dashboard/activities")
	public ApiResponse<ProjectRecentActivityListResponse> getProjectDashboardActivities(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(required = false) Integer limit
	) {
		return ApiResponse.success(
			"PROJECT_RECENT_ACTIVITY_LIST_SUCCESS",
			"프로젝트 최근 활동 목록 조회에 성공했습니다.",
			projectService.getProjectDashboardActivities(authentication.getName(), projectId, limit)
		);
	}

	@Operation(summary = "프로젝트 진행률 통계 조회", description = "프로젝트 대시보드의 진행률 통계 카드에 필요한 일정 상태별 통계를 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/dashboard/progress")
	public ApiResponse<ProjectProgressStatResponse> getProjectDashboardProgress(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"PROJECT_PROGRESS_STAT_SUCCESS",
			"프로젝트 진행률 통계 조회에 성공했습니다.",
			projectService.getProjectDashboardProgress(authentication.getName(), projectId)
		);
	}

	@Operation(summary = "프로젝트 마일스톤 목록 조회", description = "프로젝트 대시보드의 마일스톤 탭에서 사용할 마일스톤 목록을 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/dashboard/milestones")
	public ApiResponse<ProjectMilestoneListResponse> getProjectDashboardMilestones(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(required = false) String status
	) {
		return ApiResponse.success(
			"PROJECT_MILESTONE_LIST_SUCCESS",
			"프로젝트 마일스톤 목록 조회에 성공했습니다.",
			projectService.getProjectDashboardMilestones(authentication.getName(), projectId, status)
		);
	}

	@Operation(summary = "프로젝트 파트별 현황 조회", description = "프로젝트 대시보드의 파트별 현황 카드에 필요한 부서별 진행 현황을 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/{projectId}/dashboard/departments")
	public ApiResponse<ProjectDepartmentStatusResponse> getProjectDashboardDepartments(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"PROJECT_DEPARTMENT_STATUS_SUCCESS",
			"프로젝트 파트별 현황 조회에 성공했습니다.",
			projectService.getProjectDashboardDepartments(authentication.getName(), projectId)
		);
	}
}
