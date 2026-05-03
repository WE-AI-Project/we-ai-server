package com.weai.server.domain.project.request;

import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Project join request")
public record ProjectJoinRequest(
	@Schema(description = "Project invite code", example = "PJ7X2K9A")
	String projectCode,

	@Schema(description = "Department to join as", example = "BACKEND")
	@NotNull(message = "department is required.")
	ProjectDepartment department
) {
}
