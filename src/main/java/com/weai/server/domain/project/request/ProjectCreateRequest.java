package com.weai.server.domain.project.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Project create request")
public record ProjectCreateRequest(
	@Schema(description = "Project name", example = "WE&AI Enterprise", requiredMode = Schema.RequiredMode.REQUIRED)
	String projectName,

	@Schema(description = "Project description", example = "AI-based developer collaboration platform")
	@Size(max = 500, message = "description must be 500 characters or fewer.")
	String description,

	@Schema(description = "Optional repository URL linked to the project.", example = "https://github.com/example/we-ai")
	@Size(max = 500, message = "repositoryUrl must be 500 characters or fewer.")
	String repositoryUrl,

	@Schema(
		description = "Local working path for the project.",
		example = "D:\\WE_AI\\enterprise",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	@Size(max = 500, message = "localPath must be 500 characters or fewer.")
	String localPath,

	@Schema(description = "Leader department. When omitted, the first tech stack category is used.", example = "BACKEND")
	ProjectDepartment department,

	@JsonAlias("targetDate")
	@Schema(description = "Optional project deadline.", example = "2026-05-15")
	LocalDate deadlineDate,

	@ArraySchema(
		arraySchema = @Schema(description = "Tech stacks initially registered for the project."),
		schema = @Schema(implementation = ProjectTechStackRequest.class)
	)
	@Valid
	List<ProjectTechStackRequest> techStacks
) {

	public List<ProjectTechStackRequest> techStacksOrEmpty() {
		return techStacks == null ? List.of() : techStacks;
	}
}
