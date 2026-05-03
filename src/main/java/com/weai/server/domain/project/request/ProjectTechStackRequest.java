package com.weai.server.domain.project.request;

import com.weai.server.domain.project.domain.ProjectTechStackCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Project tech stack item")
public record ProjectTechStackRequest(
	@Schema(description = "Tech stack name", example = "Spring Boot")
	@NotBlank(message = "techStacks.name is required.")
	@Size(max = 50, message = "techStacks.name must be 50 characters or fewer.")
	String name,

	@Schema(description = "Tech stack version", example = "3.2.5")
	@Size(max = 30, message = "techStacks.version must be 30 characters or fewer.")
	String version,

	@Schema(description = "Tech stack category", example = "BACKEND")
	@NotNull(message = "techStacks.category is required.")
	ProjectTechStackCategory category,

	@Schema(description = "Whether this stack is required", example = "true")
	Boolean isRequired
) {

	public boolean requiredOrDefaultFalse() {
		return Boolean.TRUE.equals(isRequired);
	}
}
