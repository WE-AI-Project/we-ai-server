package com.weai.server.domain.smartcommit;

import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Smart Commit", description = "VS Code smart versioning diff capture API.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/smart-commit")
public class SmartCommitController {

	private final PendingDiffStore pendingDiffStore;
	private final UserService userService;
	private final ProjectService projectService;

	@Operation(
		summary = "Register a pending diff",
		description = "Stores a VS Code save diff so the smart commit scheduler can generate a RAG-grounded auto commit later."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED})
	@PostMapping("/pending")
	public ApiResponse<SmartCommitPendingResponse> registerPendingDiff(
		Authentication authentication,
		@Valid @RequestBody SmartCommitPendingRequest request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		pendingDiffStore.addOrUpdate(request.fileName(), request.diff());
		PendingDiffStore.PendingStateSnapshot snapshot = pendingDiffStore.snapshot();

		return ApiResponse.success(
			"SMART_COMMIT_PENDING_REGISTERED",
			"Pending smart commit diff registered successfully.",
			new SmartCommitPendingResponse(snapshot.pendingFileCount(), snapshot.lastModifiedTime())
		);
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
