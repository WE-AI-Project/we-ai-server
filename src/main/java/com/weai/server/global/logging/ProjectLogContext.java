package com.weai.server.global.logging;

import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.MDC;

public final class ProjectLogContext {

	public static final String PROJECT_ID_KEY = "projectId";
	public static final String REQUEST_ID_KEY = "requestId";
	public static final String SOURCE_KEY = "serverLogSource";

	private ProjectLogContext() {
	}

	public static Scope open(Long projectId) {
		return open(projectId, null);
	}

	public static Scope open(Long projectId, String source) {
		Map<String, String> previousContext = MDC.getCopyOfContextMap();
		if (projectId != null) {
			MDC.put(PROJECT_ID_KEY, projectId.toString());
		}
		if (source != null && !source.isBlank()) {
			MDC.put(SOURCE_KEY, source.trim());
		}
		return new Scope(previousContext);
	}

	public static Runnable wrap(Long projectId, Runnable task) {
		return wrap(projectId, null, task);
	}

	public static Runnable wrap(Long projectId, String source, Runnable task) {
		return () -> {
			try (Scope ignored = open(projectId, source)) {
				task.run();
			}
		};
	}

	public static Runnable capture(Runnable task) {
		Map<String, String> capturedContext = MDC.getCopyOfContextMap();
		return () -> runWithContext(capturedContext, task);
	}

	public static <T> Callable<T> capture(Callable<T> task) {
		Map<String, String> capturedContext = MDC.getCopyOfContextMap();
		return () -> callWithContext(capturedContext, task);
	}

	private static void runWithContext(Map<String, String> context, Runnable task) {
		Map<String, String> previousContext = MDC.getCopyOfContextMap();
		setContext(context);
		try {
			task.run();
		} finally {
			setContext(previousContext);
		}
	}

	private static <T> T callWithContext(Map<String, String> context, Callable<T> task) throws Exception {
		Map<String, String> previousContext = MDC.getCopyOfContextMap();
		setContext(context);
		try {
			return task.call();
		} finally {
			setContext(previousContext);
		}
	}

	private static void setContext(Map<String, String> context) {
		MDC.clear();
		if (context != null && !context.isEmpty()) {
			MDC.setContextMap(context);
		}
	}

	public static final class Scope implements AutoCloseable {

		private final Map<String, String> previousContext;
		private boolean closed;

		private Scope(Map<String, String> previousContext) {
			this.previousContext = previousContext;
		}

		@Override
		public void close() {
			if (!closed) {
				setContext(previousContext);
				closed = true;
			}
		}
	}
}
