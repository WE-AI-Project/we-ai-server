package com.weai.server.domain.ai.chat;

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
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Chat", description = "RAG-based project knowledge assistant API.")
@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

	private final AiChatService aiChatService;
	private final UserService userService;
	private final ProjectService projectService;

	public AiChatController(@Lazy AiChatService aiChatService, UserService userService, ProjectService projectService) {
		this.aiChatService = aiChatService;
		this.userService = userService;
		this.projectService = projectService;
	}

	@Operation(
		summary = "Ask the Oracle knowledge assistant",
		description = "Retrieves relevant project documents from ChromaDB and answers using Ollama Llama3.1."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/chat")
	public ApiResponse<ChatResponse> chat(
		Authentication authentication,
		@Valid @RequestBody ChatRequest request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		return ApiResponse.success(
			"AI_CHAT_SUCCESS",
			"AI chat completed successfully.",
			aiChatService.chat(request.projectId(), request.question())
		);
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
