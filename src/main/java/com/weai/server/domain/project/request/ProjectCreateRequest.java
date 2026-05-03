package com.weai.server.domain.project.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "프로젝트 생성 요청")
public record ProjectCreateRequest(
	@Schema(description = "프로젝트명", example = "WE&AI Enterprise", requiredMode = Schema.RequiredMode.REQUIRED)
	String projectName,

	@Schema(description = "프로젝트 설명", example = "AI 기반 개발 협업 플랫폼")
	@Size(max = 500, message = "description must be 500 characters or fewer.")
	String description,

	@Schema(
		description = "프로젝트 저장 위치. 사용자가 직접 입력하거나 폴더 선택 UI에서 고른 절대 경로 문자열을 전달합니다.",
		example = "D:\\WE_AI\\enterprise",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	@Size(max = 500, message = "localPath must be 500 characters or fewer.")
	String localPath,

	@Schema(description = "프로젝트 내 생성자 부서. 없으면 감지된 기술 스택을 기준으로 자동 보정됩니다.", example = "BACKEND")
	ProjectDepartment department,

	@JsonAlias("targetDate")
	@Schema(description = "프로젝트 마감일. 선택값이며, 응답에서는 오늘 기준 남은 일수를 함께 반환합니다.", example = "2026-05-15")
	LocalDate deadlineDate,

	@ArraySchema(
		arraySchema = @Schema(
			description = "AI 또는 프론트가 확정한 기술 스택 목록입니다. 현재 백엔드는 경로 분석이나 자동 감지를 수행하지 않고 전달받은 결과를 저장합니다."
		),
		schema = @Schema(implementation = ProjectTechStackRequest.class)
	)
	@Valid
	List<ProjectTechStackRequest> techStacks
) {

	public List<ProjectTechStackRequest> techStacksOrEmpty() {
		return techStacks == null ? List.of() : techStacks;
	}
}
