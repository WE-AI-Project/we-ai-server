package com.weai.server.domain.ai.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Project-isolated RAG chat request")
public record ChatRequest(
	@Schema(description = "Workspace/project id used to isolate RAG retrieval", example = "1")
	@NotNull(message = "projectId is required.")
	Long projectId,

	@Schema(description = "User question for the project knowledge assistant", example = "How is JWT authentication validated in this project?")
	@NotBlank(message = "question is required.")
	@Size(max = 4000, message = "question must be 4000 characters or fewer.")
	String question
) {
}
