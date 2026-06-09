package com.weai.server.domain.ai.chat;

import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Chat", description = "RAG-based project knowledge assistant API.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiChatController {

	private final AiChatService aiChatService;

	@Operation(
		summary = "Ask the Oracle knowledge assistant",
		description = "Retrieves relevant project documents from ChromaDB and answers using Ollama Llama3.1."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/chat")
	public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
		return ApiResponse.success(
			"AI_CHAT_SUCCESS",
			"AI chat completed successfully.",
			aiChatService.chat(request.question())
		);
	}
}
