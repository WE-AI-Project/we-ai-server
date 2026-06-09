package com.weai.server.domain.ai.qa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Code QA request based on a git diff")
public record QaRequest(
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
	String diff
) {
}
