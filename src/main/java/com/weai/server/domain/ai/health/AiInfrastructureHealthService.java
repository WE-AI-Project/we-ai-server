package com.weai.server.domain.ai.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiInfrastructureHealthService {

	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final String ollamaBaseUrl;
	private final String chromaBaseUrl;
	private final String accessClientId;
	private final String accessClientSecret;
	private final Duration requestTimeout;

	public AiInfrastructureHealthService(
		ObjectMapper objectMapper,
		@Value("${ai.chat.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String ollamaBaseUrl,
		@Value("${ai.chat.chroma.base-url:${CHROMA_BASE_URL:http://localhost:8000}}") String chromaBaseUrl,
		@Value("${ai.ollama.access-client-id:${OLLAMA_ACCESS_CLIENT_ID:}}") String accessClientId,
		@Value("${ai.ollama.access-client-secret:${OLLAMA_ACCESS_CLIENT_SECRET:}}") String accessClientSecret,
		@Value("${ai.health.timeout:${AI_HEALTH_TIMEOUT:PT5S}}") Duration timeout
	) {
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
		this.ollamaBaseUrl = trimTrailingSlash(ollamaBaseUrl);
		this.chromaBaseUrl = trimTrailingSlash(chromaBaseUrl);
		this.accessClientId = accessClientId;
		this.accessClientSecret = accessClientSecret;
		this.requestTimeout = timeout;
	}

	public AiInfrastructureHealthResponse check() {
		AiInfrastructureHealthResponse.ComponentStatus ollama = checkOllama();
		AiInfrastructureHealthResponse.ComponentStatus chroma = checkChroma();
		String status = "UP".equals(ollama.status()) && "UP".equals(chroma.status()) ? "UP" : "DOWN";
		return new AiInfrastructureHealthResponse(status, Instant.now(), ollama, chroma);
	}

	private AiInfrastructureHealthResponse.ComponentStatus checkOllama() {
		String endpoint = ollamaBaseUrl + "/api/tags";
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint)).timeout(requestTimeout).GET();
			if (StringUtils.hasText(accessClientId)) {
				builder.header("CF-Access-Client-Id", accessClientId.trim());
			}
			if (StringUtils.hasText(accessClientSecret)) {
				builder.header("CF-Access-Client-Secret", accessClientSecret.trim());
			}
			HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
			String contentType = response.headers().firstValue("content-type").orElse("");
			if (contentType.contains("text/html") || response.body().stripLeading().startsWith("<!DOCTYPE html")) {
				return down(endpoint, response.statusCode(), "Cloudflare Access returned an HTML login page. Configure OLLAMA_ACCESS_CLIENT_ID and OLLAMA_ACCESS_CLIENT_SECRET.");
			}
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return down(endpoint, response.statusCode(), "Ollama returned a non-success status.");
			}

			JsonNode modelsNode = objectMapper.readTree(response.body()).path("models");
			List<String> models = new ArrayList<>();
			if (modelsNode.isArray()) {
				modelsNode.forEach(model -> models.add(model.path("name").asText("unknown")));
			}
			return up(endpoint, response.statusCode(), "Ollama API is reachable.", models);
		} catch (Exception exception) {
			return down(endpoint, null, exception.getClass().getSimpleName() + ": " + exception.getMessage());
		}
	}

	private AiInfrastructureHealthResponse.ComponentStatus checkChroma() {
		String endpoint = chromaBaseUrl + "/api/v1/heartbeat";
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(requestTimeout).GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 410) {
				return down(endpoint, 410, "Chroma API v1 is unavailable. LangChain4j 0.31.0 requires Chroma 0.4.x API v1.");
			}
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return down(endpoint, response.statusCode(), "Chroma returned a non-success status.");
			}
			objectMapper.readTree(response.body());
			return up(endpoint, response.statusCode(), "Chroma API v1 is reachable.", List.of());
		} catch (Exception exception) {
			return down(endpoint, null, exception.getClass().getSimpleName() + ": " + exception.getMessage());
		}
	}

	private AiInfrastructureHealthResponse.ComponentStatus up(
		String endpoint,
		Integer httpStatus,
		String message,
		List<String> models
	) {
		return new AiInfrastructureHealthResponse.ComponentStatus("UP", endpoint, httpStatus, message, models);
	}

	private AiInfrastructureHealthResponse.ComponentStatus down(String endpoint, Integer httpStatus, String message) {
		return new AiInfrastructureHealthResponse.ComponentStatus("DOWN", endpoint, httpStatus, message, List.of());
	}

	private String trimTrailingSlash(String value) {
		return value == null ? "" : value.replaceAll("/+$", "");
	}
}
