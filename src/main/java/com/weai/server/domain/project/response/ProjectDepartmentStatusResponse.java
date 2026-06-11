package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "프로젝트 파트별 현황 응답")
public record ProjectDepartmentStatusResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@ArraySchema(schema = @Schema(implementation = DepartmentStatusItem.class))
	List<DepartmentStatusItem> departments
) {

	@Schema(description = "프로젝트 파트별 현황 항목")
	public record DepartmentStatusItem(
		@Schema(description = "부서", example = "BACKEND")
		ProjectDepartment department,

		@Schema(description = "활성 멤버 수", example = "3")
		long memberCount,

		@Schema(description = "전체 일정 수", example = "8")
		long scheduleCount,

		@Schema(description = "TODO 일정 수", example = "2")
		long todoCount,

		@Schema(description = "진행 중 일정 수", example = "3")
		long inProgressCount,

		@Schema(description = "완료 일정 수", example = "2")
		long completedScheduleCount,

		@Schema(description = "보류 일정 수", example = "1")
		long holdCount,

		@Schema(description = "진행률", example = "25")
		int progressRate
	) {
	}
}
