package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 상태 변경 응답")
public record ProjectStatusChangeResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "변경된 프로젝트 상태", example = "ARCHIVED")
	ProjectStatus status
) {

	public static ProjectStatusChangeResponse from(Project project) {
		return new ProjectStatusChangeResponse(project.getId(), project.getStatus());
	}
}
