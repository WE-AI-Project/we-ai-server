package com.weai.server.domain.ai.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import com.weai.server.domain.ai.rag.ProjectRagContext;
import com.weai.server.domain.ai.rag.ProjectRagContextService;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises AiQaService.analyze() against a stub Ollama HTTP server so we have real coverage of
 * the request/response wiring (not just the DB-persistence side already covered by
 * QaPersistenceServiceTest), catching a broken request shape or response-parsing contract before
 * it reaches a real Ollama server.
 */
class AiQaServiceTest {

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void analyzeParsesARealOllamaChatResponseIntoAQaResponse() throws IOException {
		startServer();
		server.createContext("/api/chat", exchange -> {
			String body = """
				{
				  "model": "qwen2.5-coder",
				  "created_at": "2026-01-01T00:00:00Z",
				  "message": {
				    "role": "assistant",
				    "content": "{\\"bug_report\\":\\"Null check missing\\",\\"optimization\\":\\"Use Optional\\",\\"commit_msg\\":\\"fix: guard against null user\\"}"
				  },
				  "done_reason": "stop",
				  "done": true,
				  "prompt_eval_count": 10,
				  "eval_count": 20
				}
				""";
			respond(exchange, 200, body);
		});

		ProjectRagContextService ragContextService = mock(ProjectRagContextService.class);
		when(ragContextService.retrieve(eq(1L), anyString()))
			.thenReturn(new ProjectRagContext(1L, List.of("project convention: use Optional, not null")));

		AiQaService service = new AiQaService(ragContextService, testModel());

		QaResponse response = service.analyze(1L, "diff --git a/A.java b/A.java\n- return null;\n+ return Optional.empty();");

		assertThat(response.bugReport()).isEqualTo("Null check missing");
		assertThat(response.optimization()).isEqualTo("Use Optional");
		assertThat(response.commitMsg()).isEqualTo("fix: guard against null user");
	}

	@Test
	void analyzeFailsFastWhenNoProjectRagContextExists() {
		ProjectRagContextService ragContextService = mock(ProjectRagContextService.class);
		when(ragContextService.retrieve(eq(1L), anyString()))
			.thenReturn(new ProjectRagContext(1L, List.of()));

		AiQaService service = new AiQaService(ragContextService, OllamaChatModel.builder()
			.baseUrl("http://127.0.0.1:1")
			.modelName("qwen2.5-coder")
			.timeout(Duration.ofSeconds(1))
			.build());

		assertThatThrownBy(() -> service.analyze(1L, "diff --git a/A.java b/A.java"))
			.isInstanceOf(ApiException.class);
	}

	private OllamaChatModel testModel() {
		String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName("qwen2.5-coder")
			.temperature(0.1)
			.timeout(Duration.ofSeconds(5))
			.responseFormat(ResponseFormat.JSON)
			.build();
	}

	private void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.start();
	}

	private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}
}
