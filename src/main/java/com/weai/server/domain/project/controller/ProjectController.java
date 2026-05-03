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
@Tag(name = "Projects", description = "Project creation, membership, and dashboard entry APIs.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

	private final ProjectService projectService;

	@Operation(summary = "Create project", description = "Create a project, add the creator as leader, and store its tech stacks.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NAME_REQUIRED,
		ErrorCode.PROJECT_NAME_TOO_LONG,
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
			"Project created successfully.",
			projectService.createProject(authentication.getName(), request)
		);
	}

	@Operation(summary = "Get my projects", description = "Fetch the active projects joined by the current user.")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED})
	@GetMapping("/my")
	public ApiResponse<List<MyProjectResponse>> getMyProjects(Authentication authentication) {
		List<MyProjectResponse> projects = projectService.getMyProjects(authentication.getName());
		String message = projects.isEmpty()
			? "There are no active projects joined by this user."
			: "Fetched my project list successfully.";
		return ApiResponse.success("PROJECT_LIST_SUCCESS", message, projects);
	}

	@Operation(summary = "Join project by code", description = "Join an active project with its invite code.")
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
			"Joined the project successfully.",
			projectService.joinProject(authentication.getName(), request)
		);
	}
}
