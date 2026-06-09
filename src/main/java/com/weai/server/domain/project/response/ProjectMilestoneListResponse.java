package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectMilestoneStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "프로젝트 마일스톤 목록 응답")
public record ProjectMilestoneListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@ArraySchema(schema = @Schema(implementation = MilestoneResponse.class))
	List<MilestoneResponse> milestones
) {

	@Schema(description = "프로젝트 마일스톤 항목")
	public record MilestoneResponse(
		@Schema(description = "마일스톤 ID", example = "5")
		Long milestoneId,

		@Schema(description = "마일스톤 제목", example = "백엔드 API 1차 마감")
		String title,

		@Schema(description = "마일스톤 설명", example = "핵심 API 구현 완료")
		String description,

		@Schema(description = "시작일", example = "2026-06-01")
		LocalDate startDate,

		@Schema(description = "종료일", example = "2026-06-15")
		LocalDate endDate,

		@Schema(description = "마일스톤 상태", example = "IN_PROGRESS")
		ProjectMilestoneStatus status,

		@Schema(description = "마일스톤 진행률", example = "60")
		int progressRate,

		@Schema(description = "관련 일정 수", example = "4")
		long relatedScheduleCount
	) {
	}
}
