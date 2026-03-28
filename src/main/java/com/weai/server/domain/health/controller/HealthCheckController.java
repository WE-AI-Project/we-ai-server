package com.weai.server.domain.health.controller;

import com.weai.server.domain.health.dto.HealthCheckResponse;
import com.weai.server.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서비스 상태를 확인하는 API")
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

	@Operation(summary = "헬스체크", description = "백엔드 서버의 기본 동작 상태를 확인합니다.")
	@GetMapping
	public ApiResponse<HealthCheckResponse> health() {
		return ApiResponse.success(HealthCheckResponse.up("we-ai-server"));
	}
}
