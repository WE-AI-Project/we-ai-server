package com.weai.server.domain.ai.rag;

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

@Tag(name = "AI RAG", description = "Project-isolated RAG document indexing API.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/rag")
public class ProjectRagIndexController {

	private final ProjectRagIndexService projectRagIndexService;
	private final UserService userService;
	private final ProjectService projectService;

	@Operation(
		summary = "Index a project document for RAG",
		description = "Chunks text, embeds each chunk, and stores it in ChromaDB with projectId metadata."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/documents")
	public ApiResponse<RagDocumentIndexResponse> index(
		Authentication authentication,
		@Valid @RequestBody RagDocumentIndexRequest request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		return ApiResponse.success(
			"AI_RAG_INDEX_SUCCESS",
			"RAG document indexed successfully.",
			projectRagIndexService.index(request.projectId(), request.source(), request.text())
		);
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
