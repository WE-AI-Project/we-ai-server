package com.weai.server.domain.ai.qa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Code QA request based on a git diff")
public record QaRequest(
	@Schema(description = "Workspace/project id used to isolate RAG retrieval", example = "1")
	@NotNull(message = "projectId is required.")
	Long projectId,

	@Schema(
		description = "Git diff text to analyze",
		example = """
			diff --git a/src/main/java/com/example/UserService.java b/src/main/java/com/example/UserService.java
			@@
			- return userRepository.findById(id).get();
			+ return userRepository.findById(id).orElse(null);
			"""
	)
	@NotBlank(message = "diff is required.")
	@Size(max = 200000, message = "diff must be 200000 characters or fewer.")
	String diff
) {
}
