package com.weai.server.domain.project.request;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectSchedulePriority;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Project schedule create request")
public record ProjectScheduleCreateRequest(
	@Schema(description = "Schedule title", example = "프로젝트 일정 상세 API 구현", requiredMode = Schema.RequiredMode.REQUIRED)
	@Size(max = 100, message = "title must be 100 characters or fewer.")
	String title,

	@Schema(description = "Schedule description", example = "일정 상세 조회 API 개발")
	@Size(max = 1000, message = "description must be 1000 characters or fewer.")
	String description,

	@Schema(description = "Assignee user id. Defaults to the authenticated user when omitted.", example = "1")
	Long assigneeId,

	@Schema(description = "Owning department", example = "BACKEND", requiredMode = Schema.RequiredMode.REQUIRED)
	ProjectDepartment department,

	@Schema(description = "Start date", example = "2026-05-24", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDate startDate,

	@Schema(description = "End date", example = "2026-05-24", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDate endDate,

	@Schema(description = "Priority. Defaults to MEDIUM when omitted.", example = "HIGH")
	ProjectSchedulePriority priority,

	@Schema(description = "Status. Defaults to TODO when omitted.", example = "TODO")
	ProjectScheduleStatus status
) {

	public ProjectSchedulePriority priorityOrDefault() {
		return priority == null ? ProjectSchedulePriority.MEDIUM : priority;
	}

	public ProjectScheduleStatus statusOrDefault() {
		return status == null ? ProjectScheduleStatus.TODO : status;
	}
}
