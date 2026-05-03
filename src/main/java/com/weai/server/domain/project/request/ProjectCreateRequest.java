package com.weai.server.domain.project.request;

import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Project creation request")
public record ProjectCreateRequest(
	@Schema(description = "Project name", example = "WE&AI Enterprise")
	String projectName,

	@Schema(description = "Project description", example = "AI-powered developer collaboration platform")
	@Size(max = 500, message = "description must be 500 characters or fewer.")
	String description,

	@Schema(description = "Local workspace path", example = "D:\\WE_AI\\enterprise")
	@Size(max = 500, message = "localPath must be 500 characters or fewer.")
	String localPath,

	@Schema(description = "Creator's department inside the project", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "Project start date", example = "2026-05-03")
	LocalDate startDate,

	@Schema(description = "Project target date", example = "2026-06-30")
	LocalDate targetDate,

	@ArraySchema(schema = @Schema(implementation = ProjectTechStackRequest.class))
	@Valid
	List<ProjectTechStackRequest> techStacks
) {

	public List<ProjectTechStackRequest> techStacksOrEmpty() {
		return techStacks == null ? List.of() : techStacks;
	}
}
