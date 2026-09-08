package com.weai.server.global.logging;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServerLogEventQueue {

	private final ServerLogPersistenceService persistenceService;
	private final ArrayBlockingQueue<ServerLogCaptureEvent> queue;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "server-log-db-writer");
		thread.setDaemon(true);
		return thread;
	});
	private final AtomicLong droppedCount = new AtomicLong();
	private volatile boolean running;

	public ServerLogEventQueue(
		ServerLogPersistenceService persistenceService,
		@Value("${server-log.capture.queue-capacity:10000}") int queueCapacity
	) {
		this.persistenceService = persistenceService;
		this.queue = new ArrayBlockingQueue<>(Math.max(100, queueCapacity));
	}

	@PostConstruct
	void start() {
		running = true;
		worker.submit(this::consume);
	}

	public void offer(ServerLogCaptureEvent event) {
		if (!running || queue.offer(event)) {
			return;
		}
		long dropped = droppedCount.incrementAndGet();
		if (dropped == 1 || (dropped & (dropped - 1)) == 0) {
			System.err.println("Server log DB queue is full. Dropped events=" + dropped);
		}
	}

	private void consume() {
		while (running || !queue.isEmpty()) {
			try {
				ServerLogCaptureEvent event = queue.poll(1, TimeUnit.SECONDS);
				if (event != null) {
					persistenceService.persist(event);
				}
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				break;
			} catch (RuntimeException exception) {
				System.err.println("Failed to persist captured server log: " + exception.getMessage());
			}
		}
	}

	@PreDestroy
	void shutdown() {
		running = false;
		worker.shutdown();
		try {
			if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
				worker.shutdownNow();
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			worker.shutdownNow();
		}
	}
}
