package com.weai.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
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

	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");
	private static final Pattern USER_ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Test
	void currentUserEndpointRequiresAccessToken() throws Exception {
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users/me".formatted(port)))
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.body()).contains("\"code\":\"COMMON_401\"");
		assertThat(response.headers().firstValue("x-request-id")).isPresent();
	}

	@Test
	void adminCanFetchBootstrapAdminUserById() throws Exception {
		String adminToken = issueAdminAccessToken();

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/admin/users/1".formatted(port)))
				.header("Authorization", "Bearer " + adminToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"username\":\"test-admin\"");
		assertThat(response.body()).contains("\"role\":\"ADMIN\"");
		assertThat(response.headers().firstValue("x-request-id")).isPresent();
	}

	@Test
	void signedUpUserAppearsInAdminUserList() throws Exception {
		String signupRequest = createSignupRequestBody("member-" + UUID.randomUUID().toString().substring(0, 8));

		HttpResponse<String> signUpResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/signup".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(signupRequest))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(signUpResponse.statusCode()).isEqualTo(200);
		long createdUserId = extractCreatedUserId(signUpResponse.body());

		String adminToken = issueAdminAccessToken();

		HttpResponse<String> listResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/admin/users?page=0&size=10".formatted(port)))
				.header("Authorization", "Bearer " + adminToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		HttpResponse<String> userResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/admin/users/%d".formatted(port, createdUserId)))
				.header("Authorization", "Bearer " + adminToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(listResponse.statusCode()).isEqualTo(200);
		assertThat(listResponse.body()).contains("\"content\"");
		assertThat(listResponse.body()).contains("\"page\":0");
		assertThat(listResponse.body()).contains("\"size\":10");
		assertThat(listResponse.body()).contains("\"role\":\"USER\"");
		assertThat(userResponse.statusCode()).isEqualTo(200);
		assertThat(userResponse.body()).contains("\"role\":\"USER\"");
	}

	private String issueAdminAccessToken() throws Exception {
		String loginRequestBody = """
			{
			  "username": "test-admin",
			  "password": "test-admin1234!"
			}
			""";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/login".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(loginRequestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(200);

		Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(response.body());
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private String createSignupRequestBody(String username) {
		return """
			{
			  "username": "%s",
			  "name": "Kim Coding",
			  "email": "%s@example.com",
			  "password": "password1234!"
			}
			""".formatted(username, username);
	}

	private long extractCreatedUserId(String responseBody) {
		Matcher matcher = USER_ID_PATTERN.matcher(responseBody);
		assertThat(matcher.find()).isTrue();
		return Long.parseLong(matcher.group(1));
	}
}
