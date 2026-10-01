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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final WorkspaceRagIndexer workspaceRagIndexer;
	private final UserService userService;
	private final ProjectService projectService;

	@Operation(
		summary = "Index a project document for RAG",
		description = "Chunks text, embeds each chunk, and stores it in ChromaDB with projectId metadata. "
			+ "Re-indexing the same (projectId, source) replaces the previous chunks; identical content is skipped "
			+ "(unchanged=true). Secret files (.env, keys, credentials) are rejected and credential values in config "
			+ "files are masked."
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
			projectRagIndexService.index(
				request.projectId(),
				request.source(),
				request.text(),
				RagDocumentOrigin.fromClient(request.origin())
			)
		);
	}

	@Operation(
		summary = "Remove a document's indexed RAG chunks",
		description = "Removes every previously indexed chunk for one (projectId, source) pair, e.g. when the "
			+ "source document is deleted from the project and should no longer be retrievable."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@DeleteMapping("/documents")
	public ApiResponse<RagDocumentDeleteResponse> delete(
		Authentication authentication,
		@Valid @RequestBody RagDocumentDeleteRequest request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		projectRagIndexService.delete(request.projectId(), request.source());

		return ApiResponse.success(
			"AI_RAG_DELETE_SUCCESS",
			"RAG document removed successfully.",
			new RagDocumentDeleteResponse(request.projectId(), request.source().trim())
		);
	}

	@Operation(
		summary = "List a project's indexed RAG documents",
		description = "Returns every indexed document (source, origin, chunk count, embedding model, indexed time) "
			+ "and the status of the latest workspace auto-indexing run."
	)
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED})
	@GetMapping("/projects/{projectId}/documents")
	public ApiResponse<RagDocumentListResponse> list(Authentication authentication, @PathVariable Long projectId) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(projectId, user.getId());

		return ApiResponse.success(RagDocumentListResponse.of(
			projectId,
			projectRagIndexService.list(projectId),
			workspaceRagIndexer.status(projectId).orElse(null)
		));
	}

	@Operation(
		summary = "Re-index the uploaded workspace",
		description = "Starts a background RAG indexing run over the project's uploaded workspace (also triggered "
			+ "automatically on every workspace upload). Unchanged files are skipped; poll the list endpoint for status."
	)
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED})
	@PostMapping("/projects/{projectId}/reindex-workspace")
	public ApiResponse<Void> reindexWorkspace(Authentication authentication, @PathVariable Long projectId) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(projectId, user.getId());

		workspaceRagIndexer.schedule(projectId);
		return ApiResponse.successMessage("AI_RAG_REINDEX_STARTED", "Workspace RAG indexing started.");
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
