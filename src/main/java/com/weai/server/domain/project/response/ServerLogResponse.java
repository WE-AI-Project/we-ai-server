package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ServerLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "서버 로그 항목")
public record ServerLogResponse(
	@Schema(description = "로그 ID", example = "100") Long logId,
	@Schema(description = "로그 레벨", example = "INFO") String level,
	@Schema(description = "로그 발생 원본", example = "SPRING_BOOT") String source,
	@Schema(description = "로그 메시지", example = "Started application in 3.241 seconds") String message,
	@Schema(description = "스레드명", example = "main", nullable = true) String threadName,
	@Schema(description = "로거명", example = "com.weai.server.WeAiServerApplication", nullable = true) String loggerName,
	@Schema(description = "추적 ID", example = "abc-123", nullable = true) String traceId,
	@Schema(description = "로그 생성 시각") LocalDateTime createdAt
) {
	public static ServerLogResponse from(ServerLog serverLog) {
		return new ServerLogResponse(
			serverLog.getId(),
			serverLog.getLevel().name(),
			serverLog.getSource().name(),
			serverLog.getMessage(),
			serverLog.getThreadName(),
			serverLog.getLoggerName(),
			serverLog.getTraceId(),
			serverLog.getCreatedAt()
		);
	}
}
