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
	private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("\"projectId\":(\\d+)");
	private static final Pattern USER_ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

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

	@Test
	void projectMemberCanViewDetailMembersTechStacksSchedulesAndDashboard() throws Exception {
		UserSession leader = signUpAndLogin("workspace-leader");
		UserSession member = signUpAndLogin("workspace-member");
		LocalDate deadlineDate = LocalDate.now().plusDays(30);

		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Workspace APIs Project",
			  "description": "Project for validating project workspace APIs",
			  "repositoryUrl": "https://github.com/example/workspace-apis",
			  "localPath": "D:\\\\WE_AI\\\\workspace-apis",
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
			      "name": "React",
			      "version": "18",
			      "category": "FRONTEND",
			      "isRequired": true
			    }
			  ]
			}
			""".formatted(deadlineDate));

		assertThat(createResponse.statusCode()).isEqualTo(201);
		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);

		HttpResponse<String> joinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "FRONTEND"
			}
			""".formatted(projectCode));

		assertThat(joinResponse.statusCode()).isEqualTo(200);

		HttpResponse<String> defaultAssigneeScheduleResponse = createProjectSchedule(member.accessToken(), projectId, """
			{
			  "title": "Frontend kickoff",
			  "description": "Plan the initial frontend milestone",
			  "department": "FRONTEND",
			  "startDate": "%s",
			  "endDate": "%s"
			}
			""".formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));

		assertThat(defaultAssigneeScheduleResponse.statusCode()).isEqualTo(201);
		assertThat(defaultAssigneeScheduleResponse.body()).contains("\"code\":\"PROJECT_SCHEDULE_CREATE_SUCCESS\"");
		assertThat(defaultAssigneeScheduleResponse.body()).contains("\"assigneeId\":" + member.userId());
		assertThat(defaultAssigneeScheduleResponse.body()).contains("\"priority\":\"MEDIUM\"");
		assertThat(defaultAssigneeScheduleResponse.body()).contains("\"status\":\"TODO\"");

		HttpResponse<String> assignedScheduleResponse = createProjectSchedule(leader.accessToken(), projectId, """
			{
			  "title": "Backend dashboard API",
			  "description": "Implement dashboard summary endpoint",
			  "assigneeId": %d,
			  "department": "BACKEND",
			  "startDate": "%s",
			  "endDate": "%s",
			  "priority": "HIGH",
			  "status": "DONE"
			}
			""".formatted(member.userId(), LocalDate.now().plusDays(3), LocalDate.now().plusDays(4)));

		assertThat(assignedScheduleResponse.statusCode()).isEqualTo(201);
		assertThat(assignedScheduleResponse.body()).contains("\"assigneeId\":" + member.userId());
		assertThat(assignedScheduleResponse.body()).contains("\"status\":\"DONE\"");

		HttpResponse<String> detailResponse = getProjectDetail(member.accessToken(), projectId);
		HttpResponse<String> membersResponse = getProjectMembers(member.accessToken(), projectId);
		HttpResponse<String> techStacksResponse = getProjectTechStacks(member.accessToken(), projectId);
		HttpResponse<String> schedulesResponse = getProjectSchedules(
			member.accessToken(),
			projectId,
			"?department=BACKEND&status=DONE"
		);
		HttpResponse<String> dashboardResponse = getProjectDashboard(member.accessToken(), projectId);

		assertThat(detailResponse.statusCode()).isEqualTo(200);
		assertThat(detailResponse.body()).contains("\"code\":\"PROJECT_DETAIL_SUCCESS\"");
		assertThat(detailResponse.body()).contains("\"repositoryUrl\":\"https://github.com/example/workspace-apis\"");
		assertThat(detailResponse.body()).contains("\"localPath\":\"D:\\\\WE_AI\\\\workspace-apis\"");

		assertThat(membersResponse.statusCode()).isEqualTo(200);
		assertThat(membersResponse.body()).contains("\"code\":\"PROJECT_MEMBER_LIST_SUCCESS\"");
		assertThat(membersResponse.body()).contains("\"userId\":" + leader.userId());
		assertThat(membersResponse.body()).contains("\"userId\":" + member.userId());
		assertThat(membersResponse.body()).contains("\"role\":\"LEADER\"");
		assertThat(membersResponse.body()).contains("\"role\":\"MEMBER\"");

		assertThat(techStacksResponse.statusCode()).isEqualTo(200);
		assertThat(techStacksResponse.body()).contains("\"code\":\"PROJECT_TECH_STACK_LIST_SUCCESS\"");
		assertThat(techStacksResponse.body()).contains("\"name\":\"Java\"");
		assertThat(techStacksResponse.body()).contains("\"name\":\"React\"");

		assertThat(schedulesResponse.statusCode()).isEqualTo(200);
		assertThat(schedulesResponse.body()).contains("\"code\":\"PROJECT_SCHEDULE_LIST_SUCCESS\"");
		assertThat(schedulesResponse.body()).contains("\"title\":\"Backend dashboard API\"");
		assertThat(schedulesResponse.body()).doesNotContain("\"title\":\"Frontend kickoff\"");

		assertThat(dashboardResponse.statusCode()).isEqualTo(200);
		assertThat(dashboardResponse.body()).contains("\"code\":\"PROJECT_DASHBOARD_SUCCESS\"");
		assertThat(dashboardResponse.body()).contains("\"memberCount\":2");
		assertThat(dashboardResponse.body()).contains("\"scheduleCount\":2");
		assertThat(dashboardResponse.body()).contains("\"completedScheduleCount\":1");
		assertThat(dashboardResponse.body()).contains("\"progressRate\":50");
		assertThat(dashboardResponse.body()).contains("\"department\":\"BACKEND\"");
		assertThat(dashboardResponse.body()).contains("\"department\":\"FRONTEND\"");
		assertThat(dashboardResponse.body()).contains("\"title\":\"Backend dashboard API\"");
		assertThat(dashboardResponse.body()).contains("\"title\":\"Frontend kickoff\"");
	}

	@Test
	void projectApisRejectNonMemberAccess() throws Exception {
		UserSession leader = signUpAndLogin("non-member-leader");
		UserSession outsider = signUpAndLogin("outsider");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Restricted Project",
			  "localPath": "D:\\\\WE_AI\\\\restricted-project"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		HttpResponse<String> detailResponse = getProjectDetail(outsider.accessToken(), projectId);

		assertThat(detailResponse.statusCode()).isEqualTo(403);
		assertThat(detailResponse.body()).contains("\"code\":\"PROJECT_403_2\"");
	}

	@Test
	void projectDetailReturns404ForUnknownProject() throws Exception {
		UserSession leader = signUpAndLogin("missing-project");

		HttpResponse<String> response = getProjectDetail(leader.accessToken(), 999999L);

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(response.body()).contains("\"code\":\"PROJECT_404_1\"");
	}

	@Test
	void scheduleCreateRejectsInvalidDate() throws Exception {
		UserSession leader = signUpAndLogin("invalid-schedule-date");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Schedule Date Validation",
			  "localPath": "D:\\\\WE_AI\\\\schedule-date-validation"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		HttpResponse<String> response = createProjectSchedule(leader.accessToken(), projectId, """
			{
			  "title": "Broken schedule",
			  "department": "BACKEND",
			  "startDate": "%s",
			  "endDate": "%s"
			}
			""".formatted(LocalDate.now().plusDays(5), LocalDate.now().plusDays(3)));

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"code\":\"PROJECT_400_9\"");
	}

	@Test
	void scheduleCreateRejectsAssigneeOutsideProject() throws Exception {
		UserSession leader = signUpAndLogin("assignee-leader");
		UserSession outsider = signUpAndLogin("assignee-outsider");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Assignee Validation",
			  "localPath": "D:\\\\WE_AI\\\\assignee-validation"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		HttpResponse<String> response = createProjectSchedule(leader.accessToken(), projectId, """
			{
			  "title": "Validate assignee",
			  "assigneeId": %d,
			  "department": "BACKEND",
			  "startDate": "%s",
			  "endDate": "%s"
			}
			""".formatted(outsider.userId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"code\":\"PROJECT_400_7\"");
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
		String accessToken = login(email, "password1234!");
		long userId = getCurrentUserId(accessToken);
		return new UserSession(username, email, accessToken, userId);
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

	private HttpResponse<String> getProjectDetail(String accessToken, long projectId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d".formatted(port, projectId)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private long getCurrentUserId(String accessToken) throws Exception {
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/users/me".formatted(port)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(200);
		return extractLongValue(response.body(), USER_ID_PATTERN);
	}

	private HttpResponse<String> getProjectMembers(String accessToken, long projectId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/members".formatted(port, projectId)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> getProjectTechStacks(String accessToken, long projectId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/tech-stacks".formatted(port, projectId)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> getProjectSchedules(String accessToken, long projectId, String queryString) throws Exception {
		String suffix = queryString == null ? "" : queryString;
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/schedules%s".formatted(port, projectId, suffix)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> getProjectDashboard(String accessToken, long projectId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/dashboard".formatted(port, projectId)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> createProjectSchedule(String accessToken, long projectId, String requestBody) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/schedules".formatted(port, projectId)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
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

	private long extractLongValue(String responseBody, Pattern pattern) {
		return Long.parseLong(extractValue(responseBody, pattern));
	}

	private record UserSession(String username, String email, String accessToken, long userId) {
	}
}
