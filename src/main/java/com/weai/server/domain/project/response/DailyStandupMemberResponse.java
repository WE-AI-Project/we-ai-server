package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "데일리 스탠드업 팀원별 브리핑 항목")
public record DailyStandupMemberResponse(
	@Schema(description = "이름", example = "김민혁")
	String name,

	@Schema(description = "소속 부서", example = "BACKEND")
	ProjectDepartment department,

	@ArraySchema(schema = @Schema(implementation = DailyStandupItemResponse.class))
	List<DailyStandupItemResponse> completed,

	@ArraySchema(schema = @Schema(implementation = DailyStandupItemResponse.class))
	List<DailyStandupItemResponse> inProgress,

	@ArraySchema(schema = @Schema(implementation = DailyStandupItemResponse.class))
	List<DailyStandupItemResponse> blockers,

	@Schema(description = "브리핑을 보는 사용자와 관련 있는 항목인지 여부", example = "true")
	boolean relevantToMe,

	@Schema(description = "관련 있는 이유 (relevantToMe가 true일 때만 의미 있음)", example = "블로커가 있어 팀 진행에 영향을 줄 수 있어요.")
	String relevantReason,

	@Schema(description = "관련 항목에 대해 취할 수 있는 액션 라벨", example = "블로커 확인")
	String relevantAction,

	@Schema(description = "relevantAction 클릭 시 이동할 화면 ID", example = "Calendar")
	String navigatePage,

	@Schema(description = "마지막 프로젝트 접속 시각", example = "2026-07-22T18:30:00")
	LocalDateTime lastAccessedAt
) {
}
