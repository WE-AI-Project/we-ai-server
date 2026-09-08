package com.weai.server.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.weai.server.domain.project.domain.ServerLogLevel;
import com.weai.server.domain.project.domain.ServerLogSource;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServerLogDatabaseAppenderTest {

	@Test
	void capturesProjectInfoLogForDatabaseAndSsePipeline() {
		ServerLogEventQueue queue = mock(ServerLogEventQueue.class);
		ServerLogDatabaseAppender appender = new ServerLogDatabaseAppender(queue);
		appender.start();
		ILoggingEvent loggingEvent = loggingEvent(Level.INFO, Map.of(
			ProjectLogContext.PROJECT_ID_KEY, "3",
			ProjectLogContext.REQUEST_ID_KEY, "request-1"
		));

		appender.doAppend(loggingEvent);

		ArgumentCaptor<ServerLogCaptureEvent> captor = ArgumentCaptor.forClass(ServerLogCaptureEvent.class);
		verify(queue).offer(captor.capture());
		assertThat(captor.getValue().projectId()).isEqualTo(3L);
		assertThat(captor.getValue().level()).isEqualTo(ServerLogLevel.INFO);
		assertThat(captor.getValue().source()).isEqualTo(ServerLogSource.SPRING_BOOT);
		assertThat(captor.getValue().traceId()).isEqualTo("request-1");
	}

	@Test
	void ignoresLogsWithoutProjectContext() {
		ServerLogEventQueue queue = mock(ServerLogEventQueue.class);
		ServerLogDatabaseAppender appender = new ServerLogDatabaseAppender(queue);
		appender.start();

		appender.doAppend(loggingEvent(Level.ERROR, Map.of()));

		verifyNoInteractions(queue);
	}

	private ILoggingEvent loggingEvent(Level level, Map<String, String> mdc) {
		ILoggingEvent event = mock(ILoggingEvent.class);
		when(event.getLevel()).thenReturn(level);
		when(event.getLoggerName()).thenReturn("com.weai.server.domain.chat.ChatRoomService");
		when(event.getFormattedMessage()).thenReturn("Chat room created");
		when(event.getThreadName()).thenReturn("http-nio-8080-exec-1");
		when(event.getMDCPropertyMap()).thenReturn(mdc);
		return event;
	}
}
