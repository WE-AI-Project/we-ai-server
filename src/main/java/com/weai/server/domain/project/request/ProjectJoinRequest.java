package com.weai.server.domain.project.request;

import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "프로젝트 참여 요청")
public record ProjectJoinRequest(
	@Schema(description = "8자리 참여 코드", example = "PJ7X2K9A")
	String projectCode,

	@Schema(description = "참여 부서", example = "BACKEND")
	@NotNull(message = "department is required.")
	ProjectDepartment department
) {
}
