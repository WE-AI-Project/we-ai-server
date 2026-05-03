package com.weai.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
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
class ProjectIntegrationTest {

	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");
	private static final Pattern PROJECT_CODE_PATTERN = Pattern.compile("\"projectCode\":\"([A-Z0-9]{8})\"");

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Test
	void userCanCreateProjectViewItAndAnotherUserCanJoinIt() throws Exception {
		UserSession leader = signUpAndLogin("leader");
		UserSession member = signUpAndLogin("member");
		LocalDate deadlineDate = LocalDate.now().plusDays(42);

		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "WE&AI Enterprise",
			  "description": "AI-based developer collaboration platform",
			  "localPath": "D:\\\\WE_AI\\\\enterprise",
			  "department": "BACKEND",
			  "deadlineDate": "%s",
			  "techStacks": [
			    {
			      "name": "Java",
			      "version": "17",
			      "category": "BACKEND",
			      "isRequired": true
			    },
			    {
			      "name": "Spring Boot",
			      "version": "4.0.5",
			      "category": "BACKEND",
			      "isRequired": true
			    }
			  ]
			}
			""".formatted(deadlineDate));

		assertThat(createResponse.statusCode()).isEqualTo(201);
		assertThat(createResponse.body()).contains("\"code\":\"PROJECT_CREATE_SUCCESS\"");
		assertThat(createResponse.body()).contains("\"projectName\":\"WE&AI Enterprise\"");
		assertThat(createResponse.body()).contains("\"localPath\":\"D:\\\\WE_AI\\\\enterprise\"");
		assertThat(createResponse.body()).contains("\"deadlineDate\":\"" + deadlineDate + "\"");
		assertThat(createResponse.body()).contains("\"daysRemaining\":42");
		assertThat(createResponse.body()).contains("\"techStackCount\":2");
		assertThat(createResponse.body()).contains("\"role\":\"LEADER\"");

		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);

		HttpResponse<String> leaderProjectsBeforeJoin = getMyProjects(leader.accessToken());

		assertThat(leaderProjectsBeforeJoin.statusCode()).isEqualTo(200);
		assertThat(leaderProjectsBeforeJoin.body()).contains("\"code\":\"PROJECT_LIST_SUCCESS\"");
		assertThat(leaderProjectsBeforeJoin.body()).contains("\"projectCode\":\"" + projectCode + "\"");
		assertThat(leaderProjectsBeforeJoin.body()).contains("\"deadlineDate\":\"" + deadlineDate + "\"");
		assertThat(leaderProjectsBeforeJoin.body()).contains("\"daysRemaining\":42");
		assertThat(leaderProjectsBeforeJoin.body()).contains("\"memberCount\":1");
		assertThat(leaderProjectsBeforeJoin.body()).contains("\"techStacks\":[\"Java\",\"Spring Boot\"]");

		HttpResponse<String> joinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "BACKEND"
			}
			""".formatted(projectCode));

		assertThat(joinResponse.statusCode()).isEqualTo(200);
		assertThat(joinResponse.body()).contains("\"code\":\"PROJECT_JOIN_SUCCESS\"");
		assertThat(joinResponse.body()).contains("\"projectCode\":\"" + projectCode + "\"");
		assertThat(joinResponse.body()).contains("\"role\":\"MEMBER\"");

		HttpResponse<String> leaderProjectsAfterJoin = getMyProjects(leader.accessToken());
		HttpResponse<String> memberProjects = getMyProjects(member.accessToken());

		assertThat(leaderProjectsAfterJoin.statusCode()).isEqualTo(200);
		assertThat(leaderProjectsAfterJoin.body()).contains("\"memberCount\":2");
		assertThat(memberProjects.statusCode()).isEqualTo(200);
		assertThat(memberProjects.body()).contains("\"projectName\":\"WE&AI Enterprise\"");
		assertThat(memberProjects.body()).contains("\"role\":\"MEMBER\"");
	}

	@Test
	void projectListReturnsEmptyArrayWhenUserHasNoProjects() throws Exception {
		UserSession user = signUpAndLogin("empty-projects");

		HttpResponse<String> response = getMyProjects(user.accessToken());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"code\":\"PROJECT_LIST_SUCCESS\"");
		assertThat(response.body()).contains("\"data\":[]");
		assertThat(response.body()).contains("\"message\":\"참여 중인 프로젝트가 없습니다.\"");
	}

	@Test
	void projectJoinRejectsDuplicateActiveMembership() throws Exception {
		UserSession leader = signUpAndLogin("duplicate-leader");
		UserSession member = signUpAndLogin("duplicate-member");
		String projectCode = createProjectAndExtractCode(leader.accessToken(), "Duplicate Join Project");

		HttpResponse<String> firstJoinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "BACKEND"
			}
			""".formatted(projectCode));

		HttpResponse<String> duplicateJoinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "BACKEND"
			}
			""".formatted(projectCode));

		assertThat(firstJoinResponse.statusCode()).isEqualTo(200);
		assertThat(duplicateJoinResponse.statusCode()).isEqualTo(409);
		assertThat(duplicateJoinResponse.body()).contains("\"code\":\"PROJECT_409_1\"");
	}

	@Test
	void projectCreateRejectsPastDeadline() throws Exception {
		UserSession leader = signUpAndLogin("invalid-date");
		LocalDate pastDeadline = LocalDate.now().minusDays(1);

		HttpResponse<String> response = createProject(leader.accessToken(), """
			{
			  "projectName": "Invalid Date Project",
			  "localPath": "D:\\\\WE_AI\\\\invalid-date",
			  "deadlineDate": "%s"
			}
			""".formatted(pastDeadline));

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"code\":\"PROJECT_400_3\"");
	}

	@Test
	void projectCreateRejectsBlankLocalPath() throws Exception {
		UserSession leader = signUpAndLogin("missing-path");

		HttpResponse<String> response = createProject(leader.accessToken(), """
			{
			  "projectName": "Missing Path Project",
			  "localPath": "   "
			}
			""");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"code\":\"PROJECT_400_6\"");
	}

	private UserSession signUpAndLogin(String prefix) throws Exception {
		String username = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
		String email = username + "@example.com";

		HttpResponse<String> signUpResponse = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/auth/signup".formatted(port)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
					{
					  "username": "%s",
					  "name": "Project User",
					  "email": "%s",
					  "password": "password1234!"
					}
					""".formatted(username, email)))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(signUpResponse.statusCode()).isEqualTo(201);
		return new UserSession(username, email, login(email, "password1234!"));
	}

	private String login(String email, String password) throws Exception {
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
		return extractValue(response.body(), ACCESS_TOKEN_PATTERN);
	}

	private HttpResponse<String> createProject(String accessToken, String requestBody) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects".formatted(port)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private String createProjectAndExtractCode(String accessToken, String projectName) throws Exception {
		HttpResponse<String> response = createProject(accessToken, """
			{
			  "projectName": "%s",
			  "description": "Project for integration testing",
			  "localPath": "D:\\\\WE_AI\\\\integration-%s"
			}
			""".formatted(projectName, projectName.toLowerCase().replace(" ", "-")));

		assertThat(response.statusCode()).isEqualTo(201);
		return extractValue(response.body(), PROJECT_CODE_PATTERN);
	}

	private HttpResponse<String> getMyProjects(String accessToken) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/my".formatted(port)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> joinProject(String accessToken, String requestBody) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/join".formatted(port)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private String extractValue(String responseBody, Pattern pattern) {
		Matcher matcher = pattern.matcher(responseBody);
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private record UserSession(String username, String email, String accessToken) {
	}
}
