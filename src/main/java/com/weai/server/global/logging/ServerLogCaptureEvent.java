package com.weai.server.global.logging;

import com.weai.server.domain.project.domain.ServerLogLevel;
import com.weai.server.domain.project.domain.ServerLogSource;

public record ServerLogCaptureEvent(
	Long projectId,
	ServerLogLevel level,
	ServerLogSource source,
	String message,
	String threadName,
	String loggerName,
	String traceId
) {
}
