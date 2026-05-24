package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project tech stack delete response")
public record ProjectTechStackDeleteResponse(
	@Schema(description = "Deleted tech stack id", example = "5")
	Long techStackId
) {

	public static ProjectTechStackDeleteResponse from(Long techStackId) {
		return new ProjectTechStackDeleteResponse(techStackId);
	}
}
