package com.weai.server.domain.ai.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "RAG chat request")
public record ChatRequest(
	@Schema(description = "User question for the project knowledge assistant", example = "프로젝트에서 인증 코드는 어떤 방식으로 검증되나요?")
	@NotBlank(message = "question is required.")
	String question
) {
}
