package com.weai.server.domain.ai.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AiInfrastructureHealthServiceTest {

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void reportsCloudflareHtmlAndChromaV1Removal() throws IOException {
		startServer();
		server.createContext("/api/tags", exchange -> respond(exchange, 200, "text/html", "<!DOCTYPE html><title>Sign in</title>"));
		server.createContext("/api/v1/heartbeat", exchange -> respond(exchange, 410, "application/json", "{}"));

		AiInfrastructureHealthResponse result = service().check();

		assertThat(result.status()).isEqualTo("DOWN");
		assertThat(result.ollama().message()).contains("Cloudflare Access");
		assertThat(result.chroma().message()).contains("LangChain4j 0.31.0");
	}

	@Test
	void reportsUpWhenBothExpectedApisReturnJson() throws IOException {
		startServer();
		server.createContext("/api/tags", exchange -> respond(
			exchange,
			200,
			"application/json",
			"{\"models\":[{\"name\":\"llama3.1\"},{\"name\":\"qwen2.5-coder\"}]}"
		));
		server.createContext("/api/v1/heartbeat", exchange -> respond(exchange, 200, "application/json", "{\"nanosecond heartbeat\":1}"));

		AiInfrastructureHealthResponse result = service().check();

		assertThat(result.status()).isEqualTo("UP");
		assertThat(result.ollama().models()).containsExactly("llama3.1", "qwen2.5-coder");
		assertThat(result.chroma().status()).isEqualTo("UP");
	}

	private void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.start();
	}

	private AiInfrastructureHealthService service() {
		String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
		return new AiInfrastructureHealthService(
			new ObjectMapper(),
			baseUrl,
			baseUrl,
			"",
			"",
			Duration.ofSeconds(2)
		);
	}

	private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String contentType, String body)
		throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", contentType);
		exchange.sendResponseHeaders(status, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}
}
