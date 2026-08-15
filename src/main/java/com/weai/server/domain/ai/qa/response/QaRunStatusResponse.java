package com.weai.server.domain.ai.qa.response;

import com.weai.server.domain.ai.qa.domain.QaRun;
import com.weai.server.domain.ai.qa.domain.QaRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "AI QA 실행 상태 조회 응답")
public record QaRunStatusResponse(
	@Schema(description = "QA 실행 ID", example = "1")
	Long qaRunId,

	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "QA 실행 상태", example = "RUNNING")
	QaRunStatus status,

	@Schema(description = "진행률", example = "60")
	int progressRate,

	@Schema(description = "현재 단계", example = "3")
	Integer currentStep,

	@Schema(description = "전체 단계", example = "5")
	Integer totalStep,

	@Schema(description = "시작 일시", example = "2026-08-15T14:10:00")
	LocalDateTime startedAt,

	@Schema(description = "종료 일시", example = "2026-08-15T14:20:00")
	LocalDateTime finishedAt,

	@Schema(description = "오류 메시지", example = "AI model timeout")
	String errorMessage
) {

	public static QaRunStatusResponse from(QaRun qaRun) {
		return new QaRunStatusResponse(
			qaRun.getId(),
			qaRun.getProject().getId(),
			qaRun.getStatus(),
			qaRun.getProgressRate(),
			qaRun.getCurrentStep(),
			qaRun.getTotalStep(),
			qaRun.getStartedAt(),
			qaRun.getFinishedAt(),
			qaRun.getErrorMessage()
		);
	}
}
