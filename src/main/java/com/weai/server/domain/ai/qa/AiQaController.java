package com.weai.server.domain.ai.qa;

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

@Tag(name = "AI QA", description = "Diff-based code QA and semantic commit generation API.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiQaController {

	private final AiQaService aiQaService;

	@Operation(
		summary = "Analyze code diff",
		description = "Analyzes a git diff and returns bug risk, optimization advice, and a semantic commit message."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/qa")
	public ApiResponse<QaResponse> analyze(@Valid @RequestBody QaRequest request) {
		return ApiResponse.success(
			"AI_QA_SUCCESS",
			"AI QA analysis completed successfully.",
			aiQaService.analyze(request.diff())
		);
	}
}
