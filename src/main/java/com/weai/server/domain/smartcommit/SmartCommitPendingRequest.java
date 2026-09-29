package com.weai.server.domain.smartcommit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Pending smart-commit diff captured by the VS Code extension")
public record SmartCommitPendingRequest(
	@Schema(description = "Client action type", example = "SMART_VERSIONING_SAVE")
	String actionType,

	@Schema(description = "Workspace/project id", example = "1")
	@NotNull(message = "projectId is required.")
	Long projectId,

	@Schema(description = "Saved file name or path")
	@NotBlank(message = "fileName is required.")
	@Size(max = 1000, message = "fileName must be 1000 characters or fewer.")
	String fileName,

	@Schema(description = "Diff captured on save")
	@NotBlank(message = "diff is required.")
	@Size(max = 200000, message = "diff must be 200000 characters or fewer.")
	String diff,

	@Schema(description = "Client save timestamp")
	String savedAt
) {
}
