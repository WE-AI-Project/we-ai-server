package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDashboardActivityType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "프로젝트 최근 활동 목록 응답")
public record ProjectRecentActivityListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "조회 제한 개수", example = "10")
	int limit,

	@ArraySchema(schema = @Schema(implementation = ActivityResponse.class))
	List<ActivityResponse> activities
) {

	@Schema(description = "프로젝트 최근 활동 항목")
	public record ActivityResponse(
		@Schema(description = "활동 ID", example = "schedule-10-updated")
		String activityId,

		@Schema(description = "활동 타입", example = "SCHEDULE_UPDATED")
		ProjectDashboardActivityType type,

		@Schema(description = "활동 제목", example = "일정이 수정되었습니다.")
		String title,

		@Schema(description = "활동 설명", example = "프로젝트 일정 상세 API 구현 일정이 수정되었습니다.")
		String description,

		@Schema(description = "활동 수행자 이름", example = "표재원")
		String actorName,

		@Schema(description = "활동 대상 이름", example = "프로젝트 일정 상세 API 구현")
		String targetName,

		@Schema(description = "활동 시각", example = "2026-06-09T10:30:00")
		LocalDateTime createdAt
	) {
	}
}
