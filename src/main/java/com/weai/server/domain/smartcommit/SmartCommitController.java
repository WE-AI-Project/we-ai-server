package com.weai.server.domain.smartcommit;

import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.smartcommit.domain.SynCommitType;
import com.weai.server.domain.smartcommit.response.SynCommitListResponse;
import com.weai.server.domain.smartcommit.response.SynCommitResponse;
import com.weai.server.domain.smartcommit.service.SynCommitAiService;
import com.weai.server.domain.smartcommit.service.SynCommitService;
import com.weai.server.domain.smartcommit.service.SynCommitService.PendingCommitBatch;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Smart Commit", description = "Syn Add / Syn Commit: SYNAIPSE's no-git, DB-backed versioning API.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/smart-commit")
public class SmartCommitController {

	private final SynCommitService synCommitService;
	private final SynCommitAiService synCommitAiService;
	private final UserService userService;
	private final ProjectService projectService;

	@Value("${smart-commit.commit-cooldown:PT5M}")
	private Duration commitCooldown;

	@Operation(
		summary = "Syn add",
		description = "Stages a VS Code save diff (\"syn add\") so it can later be folded into a syn commit."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED})
	@PostMapping("/pending")
	public ApiResponse<SmartCommitPendingResponse> registerPendingDiff(
		Authentication authentication,
		@Valid @RequestBody SmartCommitPendingRequest request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		return ApiResponse.success(
			"SMART_COMMIT_PENDING_REGISTERED",
			"Syn-add diff registered successfully.",
			synCommitService.registerPendingChange(request.projectId(), user.getId(), request.fileName(), request.diff())
		);
	}

	@Operation(
		summary = "Syn commit now",
		description = "Folds every currently staged (\"syn add\") diff for the project into one AI-generated syn commit."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SYN_COMMIT_NOTHING_PENDING,
		ErrorCode.SYN_COMMIT_COOLDOWN_ACTIVE,
		ErrorCode.SYN_COMMIT_GENERATION_FAILED
	})
	@PostMapping("/{projectId}/commit")
	public ApiResponse<SynCommitResponse> commitNow(Authentication authentication, @PathVariable Long projectId) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(projectId, user.getId());

		PendingCommitBatch batch = synCommitService.drainForManualCommit(projectId, Instant.now(), commitCooldown);
		try {
			SynCommitAiService.GeneratedSynCommit generated = synCommitAiService.generate(projectId, batch.combinedDiff());
			SynCommitResponse response = synCommitService.recordSynCommit(
				projectId,
				SynCommitType.MANUAL,
				generated.commitMessage(),
				generated.summary(),
				batch.changes()
			);
			return ApiResponse.success("SYN_COMMIT_CREATED", "Syn commit created successfully.", response);
		} catch (RuntimeException exception) {
			synCommitService.restorePendingChanges(projectId, batch.changes());
			if (exception instanceof ApiException apiException) {
				throw apiException;
			}
			throw new ApiException(ErrorCode.SYN_COMMIT_GENERATION_FAILED);
		}
	}

	@Operation(summary = "List syn commits", description = "Lists this project's syn commit history, newest first.")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED})
	@GetMapping("/{projectId}/commits")
	public ApiResponse<SynCommitListResponse> getSynCommits(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(projectId, user.getId());

		return ApiResponse.success(synCommitService.getSynCommits(projectId, page, size));
	}

	@Operation(summary = "Syn commit detail", description = "Returns one syn commit and every file diff folded into it.")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.SYN_COMMIT_NOT_FOUND})
	@GetMapping("/{projectId}/commits/{synCommitId}")
	public ApiResponse<SynCommitResponse> getSynCommitDetail(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long synCommitId
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(projectId, user.getId());

		return ApiResponse.success(synCommitService.getSynCommitDetail(projectId, synCommitId));
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
