package com.weai.server.domain.health.response;

import java.time.LocalDateTime;

public record HealthCheckResponse(
	String service,
	String status,
	LocalDateTime timestamp
) {

	public static HealthCheckResponse up(String service) {
		return new HealthCheckResponse(service, "UP", LocalDateTime.now());
	}
}
