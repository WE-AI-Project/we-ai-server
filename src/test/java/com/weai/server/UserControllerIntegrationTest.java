package com.weai.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.weai.server.domain.user.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Autowired
	private UserRepository userRepository;

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
	void healthEndpointAppliesCorsHeadersForAllowedOrigin() throws Exception {
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/health".formatted(port)))
				.header("Origin", "http://localhost:3000")
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("access-control-allow-origin"))
			.hasValue("http://localhost:3000");
		assertThat(response.headers().firstValue("access-control-expose-headers"))
			.hasValueSatisfying(headers -> assertThat(headers).contains("X-Request-Id"));
	}

	@Test
	void adminCanFetchBootstrapAdminUserById() throws Exception {
		String adminToken = issueAdminAccessToken();
		Long adminUserId = userRepository.findByUsername("test-admin").orElseThrow().getId();

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/admin/users/%d".formatted(port, adminUserId)))
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
		String username = "member-" + UUID.randomUUID().toString().substring(0, 8);
		String signupRequest = createSignupRequestBody(username);

		HttpResponse<String> signUpResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/signup".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(signupRequest))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(signUpResponse.statusCode()).isEqualTo(201);
		long createdUserId = userRepository.findByUsername(username).orElseThrow().getId();

		String adminToken = issueAdminAccessToken();

		HttpResponse<String> listResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/admin/users?page=0&size=100".formatted(port)))
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
		assertThat(listResponse.body()).contains("\"size\":100");
		assertThat(listResponse.body()).contains("\"username\":\"" + username + "\"");
		assertThat(listResponse.body()).contains("\"role\":\"USER\"");
		assertThat(userResponse.statusCode()).isEqualTo(200);
		assertThat(userResponse.body()).contains("\"username\":\"" + username + "\"");
		assertThat(userResponse.body()).contains("\"role\":\"USER\"");
	}

	@Test
	void userCanUpdateProfileAndFetchOwnActivityEndpoints() throws Exception {
		String username = "profile-" + UUID.randomUUID().toString().substring(0, 8);
		String email = username + "@example.com";
		String signupRequest = createSignupRequestBody(username);

		HttpResponse<String> signUpResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/signup".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(signupRequest))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(signUpResponse.statusCode()).isEqualTo(201);

		String accessToken = issueAccessToken(email, "password1234!");
		String updatedUsername = username + "-edit";

		HttpResponse<String> profileResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users/me/profile".formatted(port)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.method("PATCH", HttpRequest.BodyPublishers.ofString("""
					{
					  "username": "%s",
					  "name": "Updated User"
					}
					""".formatted(updatedUsername)))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(profileResponse.statusCode()).isEqualTo(200);
		assertThat(profileResponse.body()).contains("\"username\":\"" + updatedUsername + "\"");
		assertThat(profileResponse.body()).contains("\"name\":\"Updated User\"");

		HttpResponse<String> summaryResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users/me/activity-summary".formatted(port)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		HttpResponse<String> recentActivitiesResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users/me/recent-activities?limit=5".formatted(port)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(summaryResponse.statusCode()).isEqualTo(200);
		assertThat(summaryResponse.body()).contains("\"activeProjectCount\":0");
		assertThat(summaryResponse.body()).contains("\"assignedScheduleCount\":0");
		assertThat(recentActivitiesResponse.statusCode()).isEqualTo(200);
		assertThat(recentActivitiesResponse.body()).contains("\"limit\":5");
		assertThat(recentActivitiesResponse.body()).contains("\"activities\":[]");
	}

	private String issueAdminAccessToken() throws Exception {
		String loginRequestBody = """
			{
			  "email": "test-admin@local.we-ai",
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
		return extractAccessToken(response.body());
	}

	private String issueAccessToken(String email, String password) throws Exception {
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/login".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
					{
					  "email": "%s",
					  "password": "%s"
					}
					""".formatted(email, password)))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(200);
		return extractAccessToken(response.body());
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

	private String extractAccessToken(String responseBody) {
		String tokenMarker = "\"accessToken\":\"";
		int startIndex = responseBody.indexOf(tokenMarker);
		assertThat(startIndex).isGreaterThanOrEqualTo(0);

		int tokenValueStart = startIndex + tokenMarker.length();
		int tokenValueEnd = responseBody.indexOf('"', tokenValueStart);
		assertThat(tokenValueEnd).isGreaterThan(tokenValueStart);
		return responseBody.substring(tokenValueStart, tokenValueEnd);
	}
}
