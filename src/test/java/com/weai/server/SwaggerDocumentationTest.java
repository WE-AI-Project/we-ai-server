package com.weai.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SwaggerDocumentationTest {

	@LocalServerPort
	private int port;

	@Test
	void openApiDocsAreExposed() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:%d/v3/api-docs".formatted(port)))
			.GET()
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(contentType ->
			assertThat(contentType).contains("application/json"));
		assertThat(response.body()).contains("\"title\":\"WE AI Server API\"");
		assertThat(response.body()).contains("\"bearerAuth\"");
		assertThat(response.body()).contains("\"/api/v1/auth/login\"");
		assertThat(response.body()).contains("\"/api/v1/auth/signup\"");
		assertThat(response.body()).contains("\"/api/v1/auth/refresh\"");
		assertThat(response.body()).contains("\"/api/v1/auth/logout\"");
		assertThat(response.body()).contains("\"/api/v1/health\"");
		assertThat(response.body()).contains("\"/api/v1/users/me\"");
		assertThat(response.body()).contains("\"/api/v1/admin/users\"");
		assertThat(response.body()).contains("\"/api/v1/admin/users/{userId}\"");
		assertThat(response.body()).contains("\"COMMON_401\"");
		assertThat(response.body()).contains("\"COMMON_409\"");
		assertThat(response.body()).contains("\"Invalid request input.\"");
	}
}
