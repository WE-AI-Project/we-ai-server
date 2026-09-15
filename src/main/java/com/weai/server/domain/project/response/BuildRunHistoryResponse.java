package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.BuildRunStatus;
import com.weai.server.domain.project.domain.BuildTool;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "빌드 실행 히스토리 응답")
public record BuildRunHistoryResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "현재 페이지", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 페이지 수", example = "1")
	int totalPages,

	@Schema(description = "전체 빌드 실행 수", example = "3")
	long totalCount,

	@Schema(description = "성공 빌드 수", example = "2")
	long successCount,

	@Schema(description = "실패 빌드 수", example = "1")
	long failedCount,

	@Schema(description = "실행 중 빌드 수", example = "0")
	long runningCount,

	@ArraySchema(schema = @Schema(implementation = BuildRunSummaryResponse.class))
	List<BuildRunSummaryResponse> runs
) {

	@Schema(description = "빌드 실행 히스토리 항목")
	public record BuildRunSummaryResponse(
		@Schema(description = "빌드 실행 ID", example = "3")
		Long buildRunId,

		@Schema(description = "실행 태스크 이름", example = "test")
		String taskName,

		@Schema(description = "빌드 도구", example = "GRADLE")
		BuildTool buildTool,

		@Schema(description = "Spring profile", example = "dev", nullable = true)
		String profile,

		@Schema(description = "빌드 실행 상태", example = "SUCCESS")
		BuildRunStatus status,

		@Schema(description = "프로세스 종료 코드", example = "0", nullable = true)
		Integer exitCode,

		@Schema(description = "소요 시간(ms)", example = "8200", nullable = true)
		Long durationMs,

		@Schema(description = "요청자 사용자 ID", example = "3")
		Long requestedBy,

		@Schema(description = "요청자 이름", example = "김민혁")
		String requesterName,

		@Schema(description = "시작 시각", example = "2026-09-13T14:00:00")
		LocalDateTime startedAt,

		@Schema(description = "종료 시각", example = "2026-09-13T14:00:08", nullable = true)
		LocalDateTime finishedAt,

		@Schema(description = "생성 시각", example = "2026-09-13T14:00:00")
		LocalDateTime createdAt
	) {
	}
}
