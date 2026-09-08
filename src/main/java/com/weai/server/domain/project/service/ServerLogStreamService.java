package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.ServerLog;
import com.weai.server.domain.project.domain.ServerLogLevel;
import com.weai.server.domain.project.domain.ServerLogSource;
import com.weai.server.domain.project.response.ServerLogResponse;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ServerLogStreamService {

	private static final long HEARTBEAT_INTERVAL_SECONDS = 20L;

	private final ConcurrentHashMap<Long, Set<Subscription>> subscriptionsByProject = new ConcurrentHashMap<>();
	private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "server-log-sse-heartbeat");
		thread.setDaemon(true);
		return thread;
	});

	public SseEmitter subscribe(Long projectId, ServerLogLevel level, ServerLogSource source, String keyword) {
		SseEmitter emitter = new SseEmitter(0L);
		Subscription subscription = new Subscription(emitter, level, source, keyword);
		subscriptionsByProject.computeIfAbsent(projectId, ignored -> ConcurrentHashMap.newKeySet()).add(subscription);
		emitter.onCompletion(() -> remove(projectId, subscription));
		emitter.onTimeout(() -> remove(projectId, subscription));
		emitter.onError(ignored -> remove(projectId, subscription));

		try {
			emitter.send(SseEmitter.event().name("connected").data("server-log-stream-connected"));
			subscription.heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
				() -> sendHeartbeat(projectId, subscription),
				HEARTBEAT_INTERVAL_SECONDS,
				HEARTBEAT_INTERVAL_SECONDS,
				TimeUnit.SECONDS
			);
			return emitter;
		} catch (IOException exception) {
			remove(projectId, subscription);
			emitter.completeWithError(exception);
			throw new IllegalStateException("Failed to initialize server log stream.", exception);
		}
	}

	public void publish(ServerLog serverLog) {
		Set<Subscription> subscriptions = subscriptionsByProject.get(serverLog.getProject().getId());
		if (subscriptions == null) {
			return;
		}
		for (Subscription subscription : subscriptions) {
			if (!subscription.matches(serverLog)) {
				continue;
			}
			try {
				subscription.emitter.send(SseEmitter.event().name("log").data(ServerLogResponse.from(serverLog)));
			} catch (IOException | IllegalStateException exception) {
				remove(serverLog.getProject().getId(), subscription);
			}
		}
	}

	private void sendHeartbeat(Long projectId, Subscription subscription) {
		try {
			subscription.emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
		} catch (IOException | IllegalStateException exception) {
			remove(projectId, subscription);
		}
	}

	private void remove(Long projectId, Subscription subscription) {
		Set<Subscription> subscriptions = subscriptionsByProject.get(projectId);
		if (subscriptions != null) {
			subscriptions.remove(subscription);
			if (subscriptions.isEmpty()) {
				subscriptionsByProject.remove(projectId, subscriptions);
			}
		}
		if (subscription.heartbeatTask != null) {
			subscription.heartbeatTask.cancel(false);
		}
	}

	@PreDestroy
	void shutdown() {
		heartbeatExecutor.shutdownNow();
	}

	private static final class Subscription {

		private final SseEmitter emitter;
		private final ServerLogLevel level;
		private final ServerLogSource source;
		private final String keyword;
		private volatile ScheduledFuture<?> heartbeatTask;

		private Subscription(SseEmitter emitter, ServerLogLevel level, ServerLogSource source, String keyword) {
			this.emitter = emitter;
			this.level = level;
			this.source = source;
			this.keyword = keyword == null ? null : keyword.toLowerCase(Locale.ROOT);
		}

		private boolean matches(ServerLog serverLog) {
			return (level == null || level == serverLog.getLevel())
				&& (source == null || source == serverLog.getSource())
				&& (keyword == null || serverLog.getMessage().toLowerCase(Locale.ROOT).contains(keyword));
		}
	}
}
