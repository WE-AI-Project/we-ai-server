package com.weai.server.global.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ServerLogAppenderRegistrar {

	private static final String APPENDER_NAME = "SERVER_LOG_DATABASE";

	private final ServerLogEventQueue eventQueue;
	private final boolean captureEnabled;
	private ServerLogDatabaseAppender appender;

	public ServerLogAppenderRegistrar(
		ServerLogEventQueue eventQueue,
		@Value("${server-log.capture.enabled:true}") boolean captureEnabled
	) {
		this.eventQueue = eventQueue;
		this.captureEnabled = captureEnabled;
	}

	@EventListener(ContextRefreshedEvent.class)
	public synchronized void register() {
		if (!captureEnabled || appender != null || !(LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
			return;
		}

		Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
		if (rootLogger.getAppender(APPENDER_NAME) != null) {
			return;
		}

		ServerLogDatabaseAppender databaseAppender = new ServerLogDatabaseAppender(eventQueue);
		databaseAppender.setContext(context);
		databaseAppender.setName(APPENDER_NAME);
		databaseAppender.start();
		rootLogger.addAppender(databaseAppender);
		this.appender = databaseAppender;
	}

	@PreDestroy
	public synchronized void unregister() {
		if (appender == null) {
			return;
		}
		if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {
			context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(appender);
		}
		appender.stop();
		appender = null;
	}
}
