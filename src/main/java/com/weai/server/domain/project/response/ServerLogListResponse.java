package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "서버 로그 목록 또는 검색 결과")
public record ServerLogListResponse(
	@Schema(description = "프로젝트 ID", example = "1") Long projectId,
	@Schema(description = "ERROR 로그 개수", example = "2") long errorCount,
	@Schema(description = "WARN 로그 개수", example = "5") long warnCount,
	@Schema(description = "INFO 로그 개수", example = "120") long infoCount,
	@Schema(description = "전체 로그 개수", example = "127") long totalCount,
	@Schema(description = "현재 페이지", example = "0") int page,
	@Schema(description = "페이지 크기", example = "100") int size,
	@Schema(description = "전체 페이지 수", example = "2") int totalPages,
	@Schema(description = "적용된 검색 필터", nullable = true) ServerLogFiltersResponse filters,
	@ArraySchema(schema = @Schema(implementation = ServerLogResponse.class)) List<ServerLogResponse> logs
) {
	@Schema(description = "서버 로그 검색 필터")
	public record ServerLogFiltersResponse(
		@Schema(description = "검색어", nullable = true) String keyword,
		@Schema(description = "로그 레벨", nullable = true) String level,
		@Schema(description = "로그 발생 원본", nullable = true) String source,
		@Schema(description = "시작 일시", nullable = true) LocalDateTime startDateTime,
		@Schema(description = "종료 일시", nullable = true) LocalDateTime endDateTime
	) {
	}
}
