package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.weai.server.domain.project.response.ServerLogEntryResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ServerLogStreamingServiceTest {

	@Test
	@DisplayName("로그 푸시 시 버퍼에 정상 저장되고 최근 로그를 조회할 수 있다")
	void pushLogAndGetRecentLogs() {
		ServerLogStreamingService service = new ServerLogStreamingService();

		ServerLogEntryResponse log1 = new ServerLogEntryResponse(1L, "10:00:00.000", "INFO", "main", "c.weai.Test", "Test log 1");
		ServerLogEntryResponse log2 = new ServerLogEntryResponse(2L, "10:00:01.000", "ERROR", "main", "c.weai.Test", "Test log 2");

		service.pushLog(log1);
		service.pushLog(log2);

		List<ServerLogEntryResponse> recentLogs = service.getRecentLogs();
		assertThat(recentLogs).hasSize(2);
		assertThat(recentLogs.get(0).message()).isEqualTo("Test log 1");
		assertThat(recentLogs.get(1).message()).isEqualTo("Test log 2");
	}

	@Test
	@DisplayName("SseEmitter 구독이 정상적으로 생성된다")
	void subscribeSseEmitter() {
		ServerLogStreamingService service = new ServerLogStreamingService();

		SseEmitter emitter = service.subscribe();
		assertThat(emitter).isNotNull();

		// 구독 후 신규 로그 푸시 시 예외 없이 처리됨
		ServerLogEntryResponse log = new ServerLogEntryResponse(3L, "10:00:02.000", "WARN", "worker", "c.weai.Test", "Warning log");
		service.pushLog(log);

		service.cleanup();
	}
}
