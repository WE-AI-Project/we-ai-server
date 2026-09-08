package com.weai.server.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ProjectLogContextTest {

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void scopeAddsSchedulerProjectContextAndRestoresPreviousContext() {
		MDC.put("existing", "value");

		try (ProjectLogContext.Scope ignored = ProjectLogContext.open(9L, "AGENT")) {
			assertThat(MDC.get(ProjectLogContext.PROJECT_ID_KEY)).isEqualTo("9");
			assertThat(MDC.get(ProjectLogContext.SOURCE_KEY)).isEqualTo("AGENT");
		}

		assertThat(MDC.get(ProjectLogContext.PROJECT_ID_KEY)).isNull();
		assertThat(MDC.get("existing")).isEqualTo("value");
	}

	@Test
	void capturePropagatesCurrentMdcToAsyncTask() {
		MDC.put(ProjectLogContext.PROJECT_ID_KEY, "12");
		Runnable task = ProjectLogContext.capture((Runnable) () ->
			assertThat(MDC.get(ProjectLogContext.PROJECT_ID_KEY)).isEqualTo("12"));
		MDC.clear();

		task.run();

		assertThat(MDC.get(ProjectLogContext.PROJECT_ID_KEY)).isNull();
	}
}
