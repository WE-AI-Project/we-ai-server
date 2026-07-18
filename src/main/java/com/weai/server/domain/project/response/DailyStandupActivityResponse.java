package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "데일리 스탠드업 최근 활동 항목")
public record DailyStandupActivityResponse(
	@Schema(description = "활동 유형", example = "TECH_STACK_ADDED")
	String type,

	@Schema(description = "활동 제목", example = "기술 스택 추가")
	String title,

	@Schema(description = "활동 설명", example = "Spring Boot 기술 스택이 추가되었습니다.")
	String description,

	@Schema(description = "활동 수행자 이름", example = "김민혁")
	String actorName,

	@Schema(description = "활동 생성 시각", example = "2026-05-25T11:00:00")
	LocalDateTime createdAt
) {
}
