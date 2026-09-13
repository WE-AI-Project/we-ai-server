package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.BuildRunStatus;
import com.weai.server.domain.project.domain.BuildTool;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "빌드 태스크 실행 시작 응답")
public record BuildTaskRunResponse(
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

	@Schema(description = "빌드 실행 상태", example = "RUNNING")
	BuildRunStatus status,

	@Schema(description = "실행 명령", example = "gradlew.bat build -Dspring.profiles.active=dev")
	String command,

	@Schema(description = "요청자 사용자 ID", example = "3")
	Long requestedBy,

	@Schema(description = "요청 시각", example = "2026-09-13T13:45:00")
	LocalDateTime requestedAt
) {
}
