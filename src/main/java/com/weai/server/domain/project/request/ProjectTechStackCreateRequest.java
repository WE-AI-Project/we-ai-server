package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Project tech stack create request")
public record ProjectTechStackCreateRequest(
	@Schema(description = "Tech stack name", example = "Spring Boot", requiredMode = Schema.RequiredMode.REQUIRED)
	@Size(max = 50, message = "name must be 50 characters or fewer.")
	String name,

	@Schema(description = "Tech stack version", example = "3.2.5")
	@Size(max = 30, message = "version must be 30 characters or fewer.")
	String version,

	@Schema(description = "Tech stack category", example = "BACKEND", requiredMode = Schema.RequiredMode.REQUIRED)
	String category,

	@Schema(description = "Required flag", example = "true")
	Boolean isRequired
) {

	public boolean requiredOrDefaultFalse() {
		return Boolean.TRUE.equals(isRequired);
	}
}
