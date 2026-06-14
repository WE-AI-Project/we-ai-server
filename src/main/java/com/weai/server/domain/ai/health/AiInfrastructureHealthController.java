package com.weai.server.domain.ai.health;

import com.weai.server.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Health", description = "Checks the external Ollama and Chroma dependencies used by AI APIs.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiInfrastructureHealthController {

	private final AiInfrastructureHealthService healthService;

	@Operation(summary = "Check AI infrastructure connectivity")
	@GetMapping("/health")
	public ApiResponse<AiInfrastructureHealthResponse> health() {
		return ApiResponse.success(
			"AI_INFRASTRUCTURE_HEALTH_CHECKED",
			"AI infrastructure health check completed.",
			healthService.check()
		);
	}
}
