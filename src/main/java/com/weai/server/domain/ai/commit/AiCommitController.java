package com.weai.server.domain.ai.commit;

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

@Tag(name = "AI Commit", description = "RAG-based commit message generation API.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiCommitController {

	private final AiCommitService aiCommitService;
	private final UserService userService;
	private final ProjectService projectService;

	@Operation(
		summary = "Generate commit message candidates",
		description = "Retrieves project-isolated RAG context, then generates commit message candidates from a git diff."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/commit")
	public ApiResponse<AiCommitResponse> generate(
		Authentication authentication,
		@Valid @RequestBody AiCommitRequest request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		return ApiResponse.success(
			"AI_COMMIT_SUCCESS",
			"AI commit message generation completed successfully.",
			aiCommitService.generate(request.projectId(), request.diff(), request.files())
		);
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
