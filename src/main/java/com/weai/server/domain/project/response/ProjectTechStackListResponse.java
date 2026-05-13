package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectTechStack;
import com.weai.server.domain.project.domain.ProjectTechStackCategory;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Project tech stack list response")
public record ProjectTechStackListResponse(
	@Schema(description = "Project id", example = "1")
	Long projectId,

	@ArraySchema(schema = @Schema(implementation = TechStackResponse.class))
	List<TechStackResponse> techStacks
) {

	public static ProjectTechStackListResponse from(Long projectId, List<ProjectTechStack> techStacks) {
		return new ProjectTechStackListResponse(projectId, techStacks.stream().map(TechStackResponse::from).toList());
	}

	@Schema(description = "Project tech stack item")
	public record TechStackResponse(
		@Schema(description = "Tech stack id", example = "1")
		Long techStackId,

		@Schema(description = "Tech stack name", example = "Spring Boot")
		String name,

		@Schema(description = "Version", example = "3.2.5")
		String version,

		@Schema(description = "Category", example = "BACKEND")
		ProjectTechStackCategory category,

		@Schema(description = "Required flag", example = "true")
		boolean isRequired
	) {

		private static TechStackResponse from(ProjectTechStack techStack) {
			return new TechStackResponse(
				techStack.getId(),
				techStack.getName(),
				techStack.getVersion(),
				techStack.getCategory(),
				techStack.isRequired()
			);
		}
	}
}
