package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.BuildRunStatus;
import com.weai.server.domain.project.domain.BuildTool;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "빌드 실행 결과 응답")
public record BuildRunResultResponse(
	@Schema(description = "빌드 실행 ID", example = "1")
	Long buildRunId,

	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "실행 태스크 이름", example = "build")
	String taskName,

	@Schema(description = "빌드 도구", example = "GRADLE")
	BuildTool buildTool,

	@Schema(description = "Spring profile", example = "dev", nullable = true)
	String profile,

	@Schema(description = "빌드 실행 상태", example = "SUCCESS")
	BuildRunStatus status,

	@Schema(description = "실행 명령", example = "gradlew.bat build")
	String command,

	@Schema(description = "프로세스 종료 코드", example = "0", nullable = true)
	Integer exitCode,

	@Schema(description = "표준 출력 로그")
	String output,

	@Schema(description = "표준 에러 로그", nullable = true)
	String errorOutput,

	@Schema(description = "시작 시각", example = "2026-09-13T13:45:00")
	LocalDateTime startedAt,

	@Schema(description = "종료 시각", example = "2026-09-13T13:45:12", nullable = true)
	LocalDateTime finishedAt,

	@Schema(description = "소요 시간(ms)", example = "12000", nullable = true)
	Long durationMs,

	@Schema(description = "요청자 사용자 ID", example = "3")
	Long requestedBy,

	@Schema(description = "요청자 이름", example = "김민혁")
	String requesterName
) {
}
