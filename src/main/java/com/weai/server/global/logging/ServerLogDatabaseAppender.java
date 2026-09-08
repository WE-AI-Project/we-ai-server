package com.weai.server.global.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import com.weai.server.domain.project.domain.ServerLogLevel;
import com.weai.server.domain.project.domain.ServerLogSource;
import java.util.Locale;
import java.util.Map;

final class ServerLogDatabaseAppender extends AppenderBase<ILoggingEvent> {

	private static final int MAX_MESSAGE_LENGTH = 4_000;
	private static final String INTERNAL_LOGGER_PREFIX = "com.weai.server.global.logging.ServerLog";

	private final ServerLogEventQueue eventQueue;

	ServerLogDatabaseAppender(ServerLogEventQueue eventQueue) {
		this.eventQueue = eventQueue;
	}

	@Override
	protected void append(ILoggingEvent loggingEvent) {
		if (loggingEvent == null || loggingEvent.getLoggerName().startsWith(INTERNAL_LOGGER_PREFIX)) {
			return;
		}

		ServerLogLevel level = toServerLogLevel(loggingEvent.getLevel());
		if (level == null) {
			return;
		}

		Map<String, String> mdc = loggingEvent.getMDCPropertyMap();
		Long projectId = parseProjectId(mdc.get(ProjectLogContext.PROJECT_ID_KEY));
		if (projectId == null) {
			return;
		}

		String traceId = firstNonBlank(mdc.get("traceId"), mdc.get(ProjectLogContext.REQUEST_ID_KEY));
		eventQueue.offer(new ServerLogCaptureEvent(
			projectId,
			level,
			resolveSource(mdc.get(ProjectLogContext.SOURCE_KEY), loggingEvent.getLoggerName()),
			buildMessage(loggingEvent),
			loggingEvent.getThreadName(),
			loggingEvent.getLoggerName(),
			traceId
		));
	}

	private ServerLogLevel toServerLogLevel(Level level) {
		if (Level.ERROR.equals(level)) {
			return ServerLogLevel.ERROR;
		}
		if (Level.WARN.equals(level)) {
			return ServerLogLevel.WARN;
		}
		if (Level.INFO.equals(level)) {
			return ServerLogLevel.INFO;
		}
		return null;
	}

	private Long parseProjectId(String rawProjectId) {
		if (rawProjectId == null || rawProjectId.isBlank()) {
			return null;
		}
		try {
			long projectId = Long.parseLong(rawProjectId);
			return projectId > 0 ? projectId : null;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private ServerLogSource resolveSource(String rawSource, String loggerName) {
		if (rawSource != null && !rawSource.isBlank()) {
			try {
				return ServerLogSource.valueOf(rawSource.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
				// Fall through to logger-based source inference.
			}
		}
		String normalizedLogger = loggerName == null ? "" : loggerName.toLowerCase(Locale.ROOT);
		if (normalizedLogger.contains("smartcommit") || normalizedLogger.contains(".agent") || normalizedLogger.contains(".ai.")) {
			return ServerLogSource.AGENT;
		}
		if (normalizedLogger.contains("gradle")) {
			return ServerLogSource.GRADLE;
		}
		return ServerLogSource.SPRING_BOOT;
	}

	private String buildMessage(ILoggingEvent loggingEvent) {
		String message = loggingEvent.getFormattedMessage();
		if (message == null) {
			message = "";
		}
		if (loggingEvent.getThrowableProxy() != null) {
			message += System.lineSeparator() + ThrowableProxyUtil.asString(loggingEvent.getThrowableProxy());
		}
		return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH);
	}

	private String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		return second == null || second.isBlank() ? null : second;
	}
}
