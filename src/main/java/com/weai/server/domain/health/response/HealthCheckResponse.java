package com.weai.server.domain.health.response;

import java.time.LocalDateTime;

public record HealthCheckResponse(
	String service,
	String status,
	LocalDateTime timestamp,
	ComponentStatus database
) {

	public static HealthCheckResponse of(String service, ComponentStatus database) {
		return new HealthCheckResponse(service, database.status(), LocalDateTime.now(), database);
	}

	public record ComponentStatus(
		String status,
		String message
	) {

		public static ComponentStatus up(String message) {
			return new ComponentStatus("UP", message);
		}

		public static ComponentStatus down(String message) {
			return new ComponentStatus("DOWN", message);
		}
	}
}
