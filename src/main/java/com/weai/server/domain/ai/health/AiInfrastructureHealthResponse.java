package com.weai.server.domain.ai.health;

import java.time.Instant;
import java.util.List;

public record AiInfrastructureHealthResponse(
	String status,
	Instant checkedAt,
	ComponentStatus ollama,
	ComponentStatus chroma
) {
	public record ComponentStatus(
		String status,
		String endpoint,
		Integer httpStatus,
		String message,
		List<String> models
	) {
	}
}
