package com.weai.server.domain.health.controller;

import com.weai.server.domain.health.response.HealthCheckResponse;
import com.weai.server.domain.health.response.HealthCheckResponse.ComponentStatus;
import com.weai.server.domain.health.service.HealthCheckService;
import com.weai.server.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서비스 상태를 확인하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/health")
public class HealthCheckController {

	private final HealthCheckService healthCheckService;

	@Operation(summary = "헬스체크", description = "백엔드 서버와 데이터베이스 연결 상태를 확인합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<HealthCheckResponse>> health() {
		ComponentStatus database = healthCheckService.checkDatabase();
		HealthCheckResponse response = HealthCheckResponse.of("we-ai-server", database);

		HttpStatus httpStatus = "UP".equals(response.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
		return ResponseEntity.status(httpStatus).body(ApiResponse.success(response));
	}
}
