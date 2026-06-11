package com.weai.server.domain.ai.debate;

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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Debate", description = "Dynamic four-agent debate API powered by Ollama qwen2.5-coder and llama3.1.")
@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {

	private final AiDebateService aiDebateService;
	private final UserService userService;
	private final ProjectService projectService;

	@Operation(
		summary = "Run dynamic turn-based four-agent debate",
		description = "Oracle, Backend, Frontend, and Inspector debate over project-isolated RAG context using qwen2.5-coder and llama3.1. Inspector can end the loop with [토론 종료]."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/debate")
	public ApiResponse<DebateResponse> debate(
		Authentication authentication,
		@Valid @RequestBody EditorContextDto request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		return ApiResponse.success(
			"AI_DEBATE_SUCCESS",
			"AI debate completed successfully.",
			aiDebateService.debate(user, request.projectId(), request)
		);
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
