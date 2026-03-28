package com.weai.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Test
	void sampleUserCanBeFetched() throws Exception {
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users/1".formatted(port)))
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"name\":\"홍길동\"");
		assertThat(response.body()).contains("\"email\":\"gildong@example.com\"");
	}

	@Test
	void userCanBeCreatedAndFetchedAgain() throws Exception {
		String requestBody = """
			{
			  "name": "김코딩",
			  "email": "coding@example.com"
			}
			""";

		HttpResponse<String> createResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(createResponse.statusCode()).isEqualTo(201);

		long createdUserId = extractUserId(createResponse.body());

		HttpResponse<String> findResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users/%d".formatted(port, createdUserId)))
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(findResponse.statusCode()).isEqualTo(200);
		assertThat(findResponse.body()).contains("\"name\":\"김코딩\"");
		assertThat(findResponse.body()).contains("\"email\":\"coding@example.com\"");
	}

	private long extractUserId(String responseBody) {
		Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(responseBody);
		assertThat(matcher.find()).isTrue();
		return Long.parseLong(matcher.group(1));
	}
}
