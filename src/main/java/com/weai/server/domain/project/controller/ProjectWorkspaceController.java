package com.weai.server.domain.project.controller;

import com.weai.server.domain.ai.rag.event.WorkspaceSnapshotUploadedEvent;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.project.service.ProjectWorkspaceService;
import com.weai.server.domain.project.service.ProjectWorkspaceService.ProjectWorkspaceUploadResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(
	name = "Project Workspace",
	description = "Uploads a full project snapshot (zip) so the central server has real files to run builds and detect the tech stack against."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/workspace")
public class ProjectWorkspaceController {

	private final ProjectWorkspaceService projectWorkspaceService;
	private final ProjectService projectService;
	private final UserService userService;
	private final ApplicationEventPublisher eventPublisher;

	@Operation(
		summary = "Upload a project workspace snapshot",
		description = "Replaces the project's server-side workspace directory with the contents of the uploaded zip. "
			+ "This is a full replace, not an incremental sync - upload a complete project snapshot every time. "
			+ "Required before build execution or tech-stack detection can run for this project."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.WORKSPACE_FILE_REQUIRED,
		ErrorCode.WORKSPACE_FILE_EMPTY,
		ErrorCode.WORKSPACE_FILE_SIZE_EXCEEDED,
		ErrorCode.WORKSPACE_FILE_TYPE_NOT_ALLOWED,
		ErrorCode.WORKSPACE_SNAPSHOT_TOO_LARGE,
		ErrorCode.WORKSPACE_EXTRACTION_FAILED
	})
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<ProjectWorkspaceUploadResponse> uploadSnapshot(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "프로젝트 전체 스냅샷 zip 파일") @RequestParam(required = false) MultipartFile file
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(projectId, user.getId());

		ProjectWorkspaceUploadResponse response = projectWorkspaceService.uploadSnapshot(projectId, file);
		eventPublisher.publishEvent(new WorkspaceSnapshotUploadedEvent(projectId));

		return ApiResponse.success(
			"PROJECT_WORKSPACE_UPLOAD_SUCCESS",
			"Project workspace snapshot uploaded successfully. RAG indexing of its text files has started in the background.",
			response
		);
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
