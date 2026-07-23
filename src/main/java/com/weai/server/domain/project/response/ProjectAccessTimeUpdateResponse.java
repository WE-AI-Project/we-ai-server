package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Project access time update response")
public record ProjectAccessTimeUpdateResponse(
	@Schema(description = "Project ID", example = "1")
	Long projectId,

	@Schema(description = "User ID", example = "3")
	Long userId,

	@Schema(description = "Updated project access time", example = "2026-07-23T17:40:00")
	LocalDateTime lastAccessedAt
) {
}
