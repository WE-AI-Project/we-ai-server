package com.weai.server.domain.ai.qa;

import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI QA", description = "Diff-based code QA and semantic commit generation API.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiQaController {

	private final AiQaService aiQaService;
	private final UserService userService;
	private final ProjectService projectService;

	@Operation(
		summary = "Analyze code diff",
		description = "Retrieves project-isolated RAG context, then analyzes a git diff and returns bug risk, optimization advice, and a semantic commit message."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/qa")
	public ApiResponse<QaResponse> analyze(
		Authentication authentication,
		@Valid @RequestBody QaRequest request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		return ApiResponse.success(
			"AI_QA_SUCCESS",
			"AI QA analysis completed successfully.",
			aiQaService.analyze(request.projectId(), request.diff())
		);
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
