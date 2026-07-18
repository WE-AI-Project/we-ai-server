package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectDepartment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "데일리 스탠드업 일정 요약 항목")
public record DailyStandupItemResponse(
	@Schema(description = "항목 유형", example = "SCHEDULE")
	String type,

	@Schema(description = "항목 제목", example = "프로젝트 일정 상세 조회 API 구현")
	String title,

	@Schema(description = "항목 설명", example = "일정 상세 조회 API 작업이 완료되었습니다.")
	String description,

	@Schema(description = "담당자 이름", example = "김민혁")
	String managerName,

	@Schema(description = "담당 부서", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "마지막 변경 시각", example = "2026-05-25T10:30:00")
	LocalDateTime updatedAt
) {
}
