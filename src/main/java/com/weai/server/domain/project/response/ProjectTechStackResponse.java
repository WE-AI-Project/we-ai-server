package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectTechStack;
import com.weai.server.domain.project.domain.ProjectTechStackCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project tech stack response")
public record ProjectTechStackResponse(
	@Schema(description = "Tech stack id", example = "5")
	Long techStackId,

	@Schema(description = "Project id", example = "1")
	Long projectId,

	@Schema(description = "Tech stack name", example = "Spring Boot")
	String name,

	@Schema(description = "Tech stack version", example = "3.2.5")
	String version,

	@Schema(description = "Tech stack category", example = "BACKEND")
	ProjectTechStackCategory category,

	@Schema(description = "Required flag", example = "true")
	boolean isRequired
) {

	public static ProjectTechStackResponse from(ProjectTechStack techStack) {
		return new ProjectTechStackResponse(
			techStack.getId(),
			techStack.getProject().getId(),
			techStack.getName(),
			techStack.getVersion(),
			techStack.getCategory(),
			techStack.isRequired()
		);
	}
}
