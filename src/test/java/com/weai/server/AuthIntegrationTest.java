package com.weai.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.weai.server.domain.auth.repository.RefreshTokenRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthIntegrationTest {

	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");
	private static final Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("\"refreshToken\":\"([^\"]+)\"");

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@LocalServerPort
	private int port;

	@Test
	void userCanSignUpLoginRefreshLogoutAndFetchProfileFromDatabase() throws Exception {
		String username = "member-" + UUID.randomUUID().toString().substring(0, 8);
		String signUpRequestBody = """
			{
			  "username": "%s",
			  "name": "Kim Coding",
			  "email": "%s@example.com",
			  "password": "password1234!"
			}
			""".formatted(username, username);

		HttpResponse<String> signUpResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/signup".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(signUpRequestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(signUpResponse.statusCode()).isEqualTo(200);
		assertThat(signUpResponse.body()).contains("\"username\":\"" + username + "\"");
		assertThat(signUpResponse.body()).contains("\"role\":\"USER\"");

		TokenPair loginTokens = login(username, "password1234!");
		assertAccessTokenClaims(loginTokens.accessToken(), username + "@example.com", "USER");
		assertStoredRefreshTokenIsHashed(username, loginTokens.refreshToken());

		HttpResponse<String> meResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users/me".formatted(port)))
				.header("Authorization", "Bearer " + loginTokens.accessToken())
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(meResponse.statusCode()).isEqualTo(200);
		assertThat(meResponse.body()).contains("\"username\":\"" + username + "\"");
		assertThat(meResponse.body()).contains("\"role\":\"USER\"");

		HttpResponse<String> refreshResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/refresh".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
					{
					  "refreshToken": "%s"
					}
					""".formatted(loginTokens.refreshToken())))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(refreshResponse.statusCode()).isEqualTo(200);
		TokenPair refreshedTokens = extractTokenPair(refreshResponse.body());
		assertThat(refreshedTokens.refreshToken()).isNotEqualTo(loginTokens.refreshToken());

		HttpResponse<String> refreshedMeResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users/me".formatted(port)))
				.header("Authorization", "Bearer " + refreshedTokens.accessToken())
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(refreshedMeResponse.statusCode()).isEqualTo(200);
		assertThat(refreshedMeResponse.body()).contains("\"username\":\"" + username + "\"");

		HttpResponse<String> logoutResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/logout".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
					{
					  "refreshToken": "%s"
					}
					""".formatted(refreshedTokens.refreshToken())))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(logoutResponse.statusCode()).isEqualTo(200);
		assertThat(logoutResponse.body()).contains("\"message\":\"Logged out successfully.\"");

		HttpResponse<String> refreshAfterLogoutResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/refresh".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
					{
					  "refreshToken": "%s"
					}
					""".formatted(refreshedTokens.refreshToken())))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(refreshAfterLogoutResponse.statusCode()).isEqualTo(401);
		assertThat(refreshAfterLogoutResponse.body()).contains("\"code\":\"COMMON_401\"");
	}

	@Test
	void regularUserCannotAccessAdminOnlyUserEndpoints() throws Exception {
		String username = "viewer-" + UUID.randomUUID().toString().substring(0, 8);
		String signUpRequestBody = """
			{
			  "username": "%s",
			  "name": "View Only",
			  "email": "%s@example.com",
			  "password": "password1234!"
			}
			""".formatted(username, username);

		httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/signup".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(signUpRequestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		String accessToken = login(username, "password1234!").accessToken();

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/admin/users".formatted(port)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"code\":\"COMMON_403\"");
	}

	private TokenPair login(String username, String password) throws Exception {
		String requestBody = """
			{
			  "username": "%s",
			  "password": "%s"
			}
			""".formatted(username, password);

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/login".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(200);
		return extractTokenPair(response.body());
	}

	private TokenPair extractTokenPair(String responseBody) {
		return new TokenPair(
			extractToken(responseBody, ACCESS_TOKEN_PATTERN),
			extractToken(responseBody, REFRESH_TOKEN_PATTERN)
		);
	}

	private String extractToken(String responseBody, Pattern pattern) {
		Matcher matcher = pattern.matcher(responseBody);
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private void assertStoredRefreshTokenIsHashed(String username, String rawRefreshToken) throws Exception {
		User user = userRepository.findByUsername(username).orElseThrow();
		String expectedHash = HexFormat.of().formatHex(
			MessageDigest.getInstance("SHA-256").digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8))
		);

		String storedTokenHash = refreshTokenRepository.findByUserId(user.getId())
			.orElseThrow()
			.getTokenHash();

		assertThat(storedTokenHash).isEqualTo(expectedHash);
		assertThat(storedTokenHash).isNotEqualTo(rawRefreshToken);
	}

	private void assertAccessTokenClaims(String accessToken, String email, String role) {
		String[] tokenParts = accessToken.split("\\.");
		assertThat(tokenParts).hasSize(3);

		String payloadJson = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
		assertThat(payloadJson).contains("\"email\":\"" + email + "\"");
		assertThat(payloadJson).contains("\"role\":\"" + role + "\"");
		assertThat(payloadJson).doesNotContain("userId");
	}

	private record TokenPair(String accessToken, String refreshToken) {
	}
}
