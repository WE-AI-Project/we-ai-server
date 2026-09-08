package com.weai.server.domain.project.service;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.weai.server.domain.project.response.ServerLogEntryResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class ServerLogStreamingService {

	private static final int MAX_BUFFER_SIZE = 500;
	private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30분
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
		.withZone(ZoneId.systemDefault());

	private final AtomicLong logSequence = new AtomicLong(1);
	private final ConcurrentLinkedDeque<ServerLogEntryResponse> logBuffer = new ConcurrentLinkedDeque<>();
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	private AppenderBase<ILoggingEvent> logbackAppender;

	@PostConstruct
	public void initializeLogbackAppender() {
		try {
			ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
				org.slf4j.Logger.ROOT_LOGGER_NAME
			);

			logbackAppender = new AppenderBase<>() {
				@Override
				protected void append(ILoggingEvent event) {
					if (!isStarted()) {
						return;
					}

					String loggerName = event.getLoggerName();
					if (loggerName != null && (
						loggerName.contains("ServerLogStreamingService") ||
						loggerName.contains("HttpLoggingFilter") ||
						loggerName.startsWith("org.apache.tomcat") ||
						loggerName.startsWith("org.apache.catalina")
					)) {
						return;
					}

					ServerLogEntryResponse entry = new ServerLogEntryResponse(
						logSequence.getAndIncrement(),
						TIME_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())),
						event.getLevel().toString(),
						event.getThreadName(),
						loggerName != null ? loggerName : "root",
						event.getFormattedMessage()
					);

					pushLog(entry);
				}
			};

			logbackAppender.setContext(rootLogger.getLoggerContext());
			logbackAppender.setName("WeAiServerLogStreamAppender");
			logbackAppender.start();
			rootLogger.addAppender(logbackAppender);

			log.info("ServerLogStreamingService initialized with Logback appender.");
		} catch (Exception e) {
			log.warn("Failed to initialize Logback appender for log streaming: {}", e.getMessage());
		}
	}

	@PreDestroy
	public void cleanup() {
		if (logbackAppender != null) {
			try {
				logbackAppender.stop();
			} catch (Exception ignored) {
			}
		}

		for (SseEmitter emitter : emitters) {
			try {
				emitter.complete();
			} catch (Exception ignored) {
			}
		}
		emitters.clear();
	}

	public void pushLog(ServerLogEntryResponse entry) {
		logBuffer.addLast(entry);
		while (logBuffer.size() > MAX_BUFFER_SIZE) {
			logBuffer.pollFirst();
		}

		if (emitters.isEmpty()) {
			return;
		}

		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event()
					.name("log")
					.data(entry));
			} catch (IOException | IllegalStateException ex) {
				emitter.complete();
				emitters.remove(emitter);
			}
		}
	}

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		emitters.add(emitter);

		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> {
			emitter.complete();
			emitters.remove(emitter);
		});
		emitter.onError(e -> {
			emitter.complete();
			emitters.remove(emitter);
		});

		try {
			// 최초 연결 시 버퍼에 있는 최근 로그 목록을 init 이벤트로 전송
			List<ServerLogEntryResponse> initialLogs = new ArrayList<>(logBuffer);
			emitter.send(SseEmitter.event()
				.name("init")
				.data(initialLogs));
		} catch (IOException | IllegalStateException e) {
			emitter.complete();
			emitters.remove(emitter);
		}

		return emitter;
	}

	public List<ServerLogEntryResponse> getRecentLogs() {
		return new ArrayList<>(logBuffer);
	}
}
