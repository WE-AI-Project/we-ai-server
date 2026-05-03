package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.request.ProjectCreateRequest;
import com.weai.server.domain.project.request.ProjectJoinRequest;
import com.weai.server.domain.project.response.MyProjectResponse;
import com.weai.server.domain.project.response.ProjectCreateResponse;
import com.weai.server.domain.project.response.ProjectJoinResponse;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
