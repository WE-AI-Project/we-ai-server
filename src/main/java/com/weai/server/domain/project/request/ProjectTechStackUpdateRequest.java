package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Project tech stack update request")
public record ProjectTechStackUpdateRequest(
	@Schema(description = "Tech stack name", example = "Spring Boot")
	@Size(max = 50, message = "name must be 50 characters or fewer.")
	String name,

	@Schema(description = "Tech stack version", example = "3.3.0")
	@Size(max = 30, message = "version must be 30 characters or fewer.")
	String version,

	@Schema(description = "Tech stack category", example = "BACKEND")
	String category,

	@Schema(description = "Required flag", example = "true")
	Boolean isRequired
) {
}
