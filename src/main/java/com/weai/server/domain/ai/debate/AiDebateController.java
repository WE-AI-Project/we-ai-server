package com.weai.server.domain.ai.debate;

import com.weai.server.domain.ai.AiRequest;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Debate", description = "Multi-agent debate API powered by Ollama and LangChain4j.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiDebateController {

	private final AiDebateService aiDebateService;

	@Operation(
		summary = "Run multi-agent debate",
		description = "Oracle frames the problem, Architect and Sync analyze it, and Oracle returns the final conclusion."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/debate")
	public ApiResponse<DebateResponse> debate(@RequestBody AiRequest request) {
		return ApiResponse.success(
			"AI_DEBATE_SUCCESS",
			"AI debate completed successfully.",
			aiDebateService.debate(request == null ? null : request.getQuery())
		);
	}
}
