package com.weai.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProjectIntegrationTest {

	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");
	private static final Pattern PROJECT_CODE_PATTERN = Pattern.compile("\"projectCode\":\"([A-Z0-9]{8})\"");
	private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("\"projectId\":(\\d+)");
	private static final Pattern SCHEDULE_ID_PATTERN = Pattern.compile("\"scheduleId\":(\\d+)");
	private static final Pattern TECH_STACK_ID_PATTERN = Pattern.compile("\"techStackId\":(\\d+)");
	private static final Pattern USER_ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
	void projectDashboardActivitiesCanBeRetrieved() throws Exception {
		UserSession leader = signUpAndLogin("dashboard-activities-leader");
		UserSession member = signUpAndLogin("dashboard-activities-member");

		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Dashboard Activities Project",
			  "localPath": "D:\\\\WE_AI\\\\dashboard-activities",
			  "department": "BACKEND",
			  "techStacks": [
			    {
			      "name": "Spring Boot",
			      "category": "BACKEND",
			      "isRequired": true
			    }
			  ]
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);

		HttpResponse<String> joinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "FRONTEND"
			}
			""".formatted(projectCode));
		assertThat(joinResponse.statusCode()).isEqualTo(200);

		HttpResponse<String> scheduleResponse = createProjectSchedule(leader.accessToken(), projectId, """
			{
			  "title": "Activity schedule",
			  "description": "Generate dashboard activities",
			  "assigneeId": %d,
			  "department": "BACKEND",
			  "startDate": "%s",
			  "endDate": "%s"
			}
			""".formatted(member.userId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));
		long scheduleId = extractLongValue(scheduleResponse.body(), SCHEDULE_ID_PATTERN);

		HttpResponse<String> doneResponse = updateProjectScheduleStatus(
			leader.accessToken(),
			projectId,
			scheduleId,
			"""
			{
			  "status": "DONE"
			}
			"""
		);
		assertThat(doneResponse.statusCode()).isEqualTo(200);

		HttpResponse<String> activitiesResponse = getProjectDashboardActivities(member.accessToken(), projectId, "?limit=5");

		assertThat(activitiesResponse.statusCode()).isEqualTo(200);
		assertThat(activitiesResponse.body()).contains("\"code\":\"PROJECT_RECENT_ACTIVITY_LIST_SUCCESS\"");
		assertThat(activitiesResponse.body()).contains("\"limit\":5");
		assertThat(activitiesResponse.body()).contains("\"type\":\"PROJECT_CREATED\"");
		assertThat(activitiesResponse.body()).contains("\"type\":\"MEMBER_JOINED\"");
		assertThat(activitiesResponse.body()).contains("\"type\":\"SCHEDULE_DONE\"");
		assertThat(activitiesResponse.body()).contains("\"type\":\"TECH_STACK_ADDED\"");
	}

	@Test
	void projectDashboardProgressCanBeRetrieved() throws Exception {
		UserSession leader = signUpAndLogin("dashboard-progress-leader");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Dashboard Progress Project",
			  "localPath": "D:\\\\WE_AI\\\\dashboard-progress",
			  "department": "BACKEND"
			}
			""");
		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);

		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("TODO item", "BACKEND", "TODO", LocalDate.now().plusDays(1)));
		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("IN PROGRESS item", "BACKEND", "IN_PROGRESS", LocalDate.now().plusDays(2)));
		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("DONE item", "BACKEND", "DONE", LocalDate.now().plusDays(3)));
		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("COMPLETED item", "BACKEND", "COMPLETED", LocalDate.now().plusDays(4)));
		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("HOLD item", "BACKEND", "HOLD", LocalDate.now().plusDays(5)));

		HttpResponse<String> progressResponse = getProjectDashboardProgress(leader.accessToken(), projectId);

		assertThat(progressResponse.statusCode()).isEqualTo(200);
		assertThat(progressResponse.body()).contains("\"code\":\"PROJECT_PROGRESS_STAT_SUCCESS\"");
		assertThat(progressResponse.body()).contains("\"totalScheduleCount\":5");
		assertThat(progressResponse.body()).contains("\"todoCount\":1");
		assertThat(progressResponse.body()).contains("\"inProgressCount\":1");
		assertThat(progressResponse.body()).contains("\"doneCount\":1");
		assertThat(progressResponse.body()).contains("\"completedCount\":1");
		assertThat(progressResponse.body()).contains("\"holdCount\":1");
		assertThat(progressResponse.body()).contains("\"completedWorkCount\":2");
		assertThat(progressResponse.body()).contains("\"remainingScheduleCount\":3");
		assertThat(progressResponse.body()).contains("\"progressRate\":40");
	}

	@Test
	void projectDashboardProgressReturnsZeroWhenThereAreNoSchedules() throws Exception {
		UserSession leader = signUpAndLogin("dashboard-progress-zero-leader");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "No Schedule Project",
			  "localPath": "D:\\\\WE_AI\\\\no-schedule-project",
			  "department": "BACKEND"
			}
			""");
		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);

		HttpResponse<String> progressResponse = getProjectDashboardProgress(leader.accessToken(), projectId);

		assertThat(progressResponse.statusCode()).isEqualTo(200);
		assertThat(progressResponse.body()).contains("\"totalScheduleCount\":0");
		assertThat(progressResponse.body()).contains("\"completedWorkCount\":0");
		assertThat(progressResponse.body()).contains("\"remainingScheduleCount\":0");
		assertThat(progressResponse.body()).contains("\"progressRate\":0");
	}

	@Test
	void projectDashboardMilestonesCanBeRetrieved() throws Exception {
		UserSession leader = signUpAndLogin("dashboard-milestones-leader");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Dashboard Milestones Project",
			  "localPath": "D:\\\\WE_AI\\\\dashboard-milestones",
			  "department": "BACKEND"
			}
			""");
		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);

		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("Milestone API", "BACKEND", "IN_PROGRESS", LocalDate.now().plusDays(1)));
		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("Milestone QA", "QA", "TODO", LocalDate.now().plusDays(2)));
		insertMilestone(
			projectId,
			"1차 마일스톤",
			"핵심 기능 완료",
			LocalDate.now(),
			LocalDate.now().plusDays(7),
			"IN_PROGRESS",
			55
		);

		HttpResponse<String> milestonesResponse = getProjectDashboardMilestones(
			leader.accessToken(),
			projectId,
			"?status=IN_PROGRESS"
		);

		assertThat(milestonesResponse.statusCode()).isEqualTo(200);
		assertThat(milestonesResponse.body()).contains("\"code\":\"PROJECT_MILESTONE_LIST_SUCCESS\"");
		assertThat(milestonesResponse.body()).contains("\"title\":\"1차 마일스톤\"");
		assertThat(milestonesResponse.body()).contains("\"status\":\"IN_PROGRESS\"");
		assertThat(milestonesResponse.body()).contains("\"progressRate\":55");
		assertThat(milestonesResponse.body()).contains("\"relatedScheduleCount\":2");
	}

	@Test
	void projectDashboardDepartmentsCanBeRetrieved() throws Exception {
		UserSession leader = signUpAndLogin("dashboard-departments-leader");
		UserSession frontendMember = signUpAndLogin("dashboard-departments-member");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Dashboard Departments Project",
			  "localPath": "D:\\\\WE_AI\\\\dashboard-departments",
			  "department": "BACKEND"
			}
			""");
		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);

		HttpResponse<String> joinResponse = joinProject(frontendMember.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "FRONTEND"
			}
			""".formatted(projectCode));
		assertThat(joinResponse.statusCode()).isEqualTo(200);

		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("Backend done", "BACKEND", "DONE", LocalDate.now().plusDays(1)));
		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("Backend todo", "BACKEND", "TODO", LocalDate.now().plusDays(2)));
		createProjectSchedule(leader.accessToken(), projectId, scheduleBody("Frontend completed", "FRONTEND", "COMPLETED", LocalDate.now().plusDays(3)));

		HttpResponse<String> departmentsResponse = getProjectDashboardDepartments(leader.accessToken(), projectId);

		assertThat(departmentsResponse.statusCode()).isEqualTo(200);
		assertThat(departmentsResponse.body()).contains("\"code\":\"PROJECT_DEPARTMENT_STATUS_SUCCESS\"");
		assertThat(departmentsResponse.body()).contains("\"department\":\"BACKEND\"");
		assertThat(departmentsResponse.body()).contains("\"memberCount\":1");
		assertThat(departmentsResponse.body()).contains("\"scheduleCount\":2");
		assertThat(departmentsResponse.body()).contains("\"todoCount\":1");
		assertThat(departmentsResponse.body()).contains("\"completedScheduleCount\":1");
		assertThat(departmentsResponse.body()).contains("\"progressRate\":50");
		assertThat(departmentsResponse.body()).contains("\"department\":\"FRONTEND\"");
		assertThat(departmentsResponse.body()).contains("\"scheduleCount\":1");
	}

	@Test
	void projectDashboardDepartmentsReturnZeroProgressWhenDepartmentHasNoSchedules() throws Exception {
		UserSession leader = signUpAndLogin("dashboard-departments-zero-leader");
		UserSession designMember = signUpAndLogin("dashboard-departments-zero-member");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Department Zero Project",
			  "localPath": "D:\\\\WE_AI\\\\department-zero",
			  "department": "BACKEND"
			}
			""");
		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);

		HttpResponse<String> joinResponse = joinProject(designMember.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "DESIGN"
			}
			""".formatted(projectCode));
		assertThat(joinResponse.statusCode()).isEqualTo(200);

		HttpResponse<String> departmentsResponse = getProjectDashboardDepartments(leader.accessToken(), projectId);

		assertThat(departmentsResponse.statusCode()).isEqualTo(200);
		assertThat(departmentsResponse.body()).contains("\"department\":\"DESIGN\"");
		assertThat(departmentsResponse.body()).contains("\"scheduleCount\":0");
		assertThat(departmentsResponse.body()).contains("\"progressRate\":0");
	}

	@Test
	void projectDashboardEndpointsReturn404WhenProjectDoesNotExist() throws Exception {
		UserSession user = signUpAndLogin("dashboard-not-found-user");

		HttpResponse<String> progressResponse = getProjectDashboardProgress(user.accessToken(), 999999L);

		assertThat(progressResponse.statusCode()).isEqualTo(404);
		assertThat(progressResponse.body()).contains("\"code\":\"PROJECT_404_1\"");
	}

	@Test
	void projectDashboardEndpointsReturn403WhenUserIsNotProjectMember() throws Exception {
		UserSession leader = signUpAndLogin("dashboard-forbidden-leader");
		UserSession outsider = signUpAndLogin("dashboard-forbidden-outsider");

		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Forbidden Dashboard Project",
			  "localPath": "D:\\\\WE_AI\\\\dashboard-forbidden",
			  "department": "BACKEND"
			}
			""");
		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);

		HttpResponse<String> progressResponse = getProjectDashboardProgress(outsider.accessToken(), projectId);

		assertThat(progressResponse.statusCode()).isEqualTo(403);
		assertThat(progressResponse.body()).contains("\"code\":\"PROJECT_403_2\"");
	}

	@Test
	void projectLeaderCanUpdateProjectInformation() throws Exception {
		UserSession leader = signUpAndLogin("project-settings-update-leader");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Initial Settings Project",
			  "description": "Initial description",
			  "repositoryUrl": "https://github.com/example/initial-settings",
			  "localPath": "D:\\\\WE_AI\\\\initial-settings"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		HttpResponse<String> updateResponse = updateProject(leader.accessToken(), projectId, """
			{
			  "projectName": "Synaipse Project",
			  "description": "AI 기반 개발 협업 플랫폼",
			  "repositoryUrl": "https://github.com/example/synaipse",
			  "localPath": "D:\\\\Synaipse",
			  "startDate": "2026-05-01",
			  "targetDate": "2026-06-30",
			  "status": "ACTIVE"
			}
			""");

		assertThat(updateResponse.statusCode()).isEqualTo(200);
		assertThat(updateResponse.body()).contains("\"code\":\"PROJECT_UPDATE_SUCCESS\"");
		assertThat(updateResponse.body()).contains("\"projectId\":" + projectId);
		assertThat(updateResponse.body()).contains("\"projectName\":\"Synaipse Project\"");
		assertThat(updateResponse.body()).contains("\"description\":\"AI 기반 개발 협업 플랫폼\"");
		assertThat(updateResponse.body()).contains("\"repositoryUrl\":\"https://github.com/example/synaipse\"");
		assertThat(updateResponse.body()).contains("\"localPath\":\"D:\\\\Synaipse\"");
		assertThat(updateResponse.body()).contains("\"status\":\"ACTIVE\"");
		assertThat(updateResponse.body()).contains("\"startDate\":\"2026-05-01\"");
		assertThat(updateResponse.body()).contains("\"targetDate\":\"2026-06-30\"");
		assertThat(updateResponse.body()).contains("\"updatedAt\":");
	}

	@Test
	void projectUpdateFailsWhenUserIsNotLeader() throws Exception {
		UserSession leader = signUpAndLogin("project-settings-non-leader");
		UserSession member = signUpAndLogin("project-settings-member");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Leader Only Project",
			  "localPath": "D:\\\\WE_AI\\\\leader-only-project"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);
		HttpResponse<String> joinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "BACKEND"
			}
			""".formatted(projectCode));
		assertThat(joinResponse.statusCode()).isEqualTo(200);

		HttpResponse<String> updateResponse = updateProject(member.accessToken(), projectId, """
			{
			  "projectName": "Should Fail Update"
			}
			""");

		assertThat(updateResponse.statusCode()).isEqualTo(403);
		assertThat(updateResponse.body()).contains("\"code\":\"PROJECT_403_3\"");
	}

	@Test
	void projectMemberCanViewProjectMemberDetail() throws Exception {
		UserSession leader = signUpAndLogin("member-detail-leader");
		UserSession member = signUpAndLogin("member-detail-member");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Member Detail Project",
			  "localPath": "D:\\\\WE_AI\\\\member-detail-project"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);
		HttpResponse<String> joinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "FRONTEND"
			}
			""".formatted(projectCode));
		assertThat(joinResponse.statusCode()).isEqualTo(200);

		HttpResponse<String> membersResponse = getProjectMembers(leader.accessToken(), projectId);
		long memberProjectMemberId = extractProjectMemberIdForUser(membersResponse.body(), member.userId());

		HttpResponse<String> detailResponse = getProjectMemberDetail(member.accessToken(), projectId, memberProjectMemberId);

		assertThat(detailResponse.statusCode()).isEqualTo(200);
		assertThat(detailResponse.body()).contains("\"code\":\"PROJECT_MEMBER_DETAIL_SUCCESS\"");
		assertThat(detailResponse.body()).contains("\"projectMemberId\":" + memberProjectMemberId);
		assertThat(detailResponse.body()).contains("\"userId\":" + member.userId());
		assertThat(detailResponse.body()).contains("\"name\":\"Project User\"");
		assertThat(detailResponse.body()).contains("\"email\":\"" + member.email() + "\"");
		assertThat(detailResponse.body()).contains("\"role\":\"MEMBER\"");
		assertThat(detailResponse.body()).contains("\"department\":\"FRONTEND\"");
		assertThat(detailResponse.body()).contains("\"status\":\"ACTIVE\"");
		assertThat(detailResponse.body()).contains("\"joinedAt\":");
	}

	@Test
	void projectLeaderCanUpdateProjectMemberRole() throws Exception {
		UserSession leader = signUpAndLogin("member-role-leader");
		UserSession member = signUpAndLogin("member-role-member");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Member Role Project",
			  "localPath": "D:\\\\WE_AI\\\\member-role-project"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);
		HttpResponse<String> joinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "BACKEND"
			}
			""".formatted(projectCode));
		assertThat(joinResponse.statusCode()).isEqualTo(200);

		HttpResponse<String> membersResponse = getProjectMembers(leader.accessToken(), projectId);
		long memberProjectMemberId = extractProjectMemberIdForUser(membersResponse.body(), member.userId());

		HttpResponse<String> updateRoleResponse = updateProjectMemberRole(leader.accessToken(), projectId, memberProjectMemberId, """
			{
			  "role": "GUEST"
			}
			""");

		assertThat(updateRoleResponse.statusCode()).isEqualTo(200);
		assertThat(updateRoleResponse.body()).contains("\"code\":\"PROJECT_MEMBER_ROLE_UPDATE_SUCCESS\"");
		assertThat(updateRoleResponse.body()).contains("\"projectMemberId\":" + memberProjectMemberId);
		assertThat(updateRoleResponse.body()).contains("\"userId\":" + member.userId());
		assertThat(updateRoleResponse.body()).contains("\"role\":\"GUEST\"");
		assertThat(updateRoleResponse.body()).contains("\"department\":\"BACKEND\"");
		assertThat(updateRoleResponse.body()).contains("\"status\":\"ACTIVE\"");
	}

	@Test
	void projectMemberRoleUpdatePreventsDemotingOnlyLeader() throws Exception {
		UserSession leader = signUpAndLogin("only-leader-role");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Only Leader Project",
			  "localPath": "D:\\\\WE_AI\\\\only-leader-project"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		HttpResponse<String> membersResponse = getProjectMembers(leader.accessToken(), projectId);
		long leaderProjectMemberId = extractProjectMemberIdForUser(membersResponse.body(), leader.userId());

		HttpResponse<String> updateRoleResponse = updateProjectMemberRole(leader.accessToken(), projectId, leaderProjectMemberId, """
			{
			  "role": "MEMBER"
			}
			""");

		assertThat(updateRoleResponse.statusCode()).isEqualTo(400);
		assertThat(updateRoleResponse.body()).contains("\"code\":\"PROJECT_400_19\"");
	}

	@Test
	void projectLeaderCanUpdateProjectMemberDepartment() throws Exception {
		UserSession leader = signUpAndLogin("member-department-leader");
		UserSession member = signUpAndLogin("member-department-member");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Member Department Project",
			  "localPath": "D:\\\\WE_AI\\\\member-department-project"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);
		HttpResponse<String> joinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "BACKEND"
			}
			""".formatted(projectCode));
		assertThat(joinResponse.statusCode()).isEqualTo(200);

		HttpResponse<String> membersResponse = getProjectMembers(leader.accessToken(), projectId);
		long memberProjectMemberId = extractProjectMemberIdForUser(membersResponse.body(), member.userId());

		HttpResponse<String> updateDepartmentResponse = updateProjectMemberDepartment(
			leader.accessToken(),
			projectId,
			memberProjectMemberId,
			"""
				{
				  "department": "FRONTEND"
				}
				"""
		);

		assertThat(updateDepartmentResponse.statusCode()).isEqualTo(200);
		assertThat(updateDepartmentResponse.body()).contains("\"code\":\"PROJECT_MEMBER_DEPARTMENT_UPDATE_SUCCESS\"");
		assertThat(updateDepartmentResponse.body()).contains("\"projectMemberId\":" + memberProjectMemberId);
		assertThat(updateDepartmentResponse.body()).contains("\"userId\":" + member.userId());
		assertThat(updateDepartmentResponse.body()).contains("\"role\":\"MEMBER\"");
		assertThat(updateDepartmentResponse.body()).contains("\"department\":\"FRONTEND\"");
		assertThat(updateDepartmentResponse.body()).contains("\"status\":\"ACTIVE\"");
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

	@Test
	void projectMemberCanViewUpdateChangeStatusAndDeleteSchedule() throws Exception {
		UserSession leader = signUpAndLogin("schedule-ops-leader");
		UserSession member = signUpAndLogin("schedule-ops-member");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Schedule Operations Project",
			  "localPath": "D:\\\\WE_AI\\\\schedule-operations-project"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		String projectCode = extractValue(createResponse.body(), PROJECT_CODE_PATTERN);

		HttpResponse<String> joinResponse = joinProject(member.accessToken(), """
			{
			  "projectCode": "%s",
			  "department": "BACKEND"
			}
			""".formatted(projectCode));
		assertThat(joinResponse.statusCode()).isEqualTo(200);

		HttpResponse<String> scheduleCreateResponse = createProjectSchedule(leader.accessToken(), projectId, """
			{
			  "title": "프로젝트 일정 상세 API 구현",
			  "description": "일정 상세 조회 API 개발",
			  "assigneeId": %d,
			  "department": "BACKEND",
			  "startDate": "%s",
			  "endDate": "%s",
			  "priority": "HIGH",
			  "status": "TODO"
			}
			""".formatted(member.userId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)));
		assertThat(scheduleCreateResponse.statusCode()).isEqualTo(201);

		long scheduleId = extractLongValue(scheduleCreateResponse.body(), SCHEDULE_ID_PATTERN);

		HttpResponse<String> detailResponse = getProjectScheduleDetail(member.accessToken(), projectId, scheduleId);
		assertThat(detailResponse.statusCode()).isEqualTo(200);
		assertThat(detailResponse.body()).contains("\"code\":\"PROJECT_SCHEDULE_DETAIL_SUCCESS\"");
		assertThat(detailResponse.body()).contains("\"scheduleId\":" + scheduleId);
		assertThat(detailResponse.body()).contains("\"projectId\":" + projectId);
		assertThat(detailResponse.body()).contains("\"title\":\"프로젝트 일정 상세 API 구현\"");

		HttpResponse<String> updateResponse = updateProjectSchedule(leader.accessToken(), projectId, scheduleId, """
			{
			  "title": "프로젝트 일정 수정 API 구현",
			  "description": "일정 수정 기능 개발",
			  "assigneeId": %d,
			  "department": "BACKEND",
			  "startDate": "%s",
			  "endDate": "%s",
			  "priority": "HIGH",
			  "status": "IN_PROGRESS"
			}
			""".formatted(member.userId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));
		assertThat(updateResponse.statusCode()).isEqualTo(200);
		assertThat(updateResponse.body()).contains("\"code\":\"PROJECT_SCHEDULE_UPDATE_SUCCESS\"");
		assertThat(updateResponse.body()).contains("\"title\":\"프로젝트 일정 수정 API 구현\"");
		assertThat(updateResponse.body()).contains("\"status\":\"IN_PROGRESS\"");
		assertThat(updateResponse.body()).contains("\"updatedAt\":");

		HttpResponse<String> statusUpdateResponse = updateProjectScheduleStatus(member.accessToken(), projectId, scheduleId, """
			{
			  "status": "DONE"
			}
			""");
		assertThat(statusUpdateResponse.statusCode()).isEqualTo(200);
		assertThat(statusUpdateResponse.body()).contains("\"code\":\"PROJECT_SCHEDULE_STATUS_UPDATE_SUCCESS\"");
		assertThat(statusUpdateResponse.body()).contains("\"status\":\"DONE\"");

		HttpResponse<String> deleteResponse = deleteProjectSchedule(leader.accessToken(), projectId, scheduleId);
		assertThat(deleteResponse.statusCode()).isEqualTo(200);
		assertThat(deleteResponse.body()).contains("\"code\":\"PROJECT_SCHEDULE_DELETE_SUCCESS\"");
		assertThat(deleteResponse.body()).contains("\"scheduleId\":" + scheduleId);

		HttpResponse<String> detailAfterDeleteResponse = getProjectScheduleDetail(leader.accessToken(), projectId, scheduleId);
		assertThat(detailAfterDeleteResponse.statusCode()).isEqualTo(404);
		assertThat(detailAfterDeleteResponse.body()).contains("\"code\":\"PROJECT_404_3\"");
	}

	@Test
	void scheduleStatusUpdateRejectsMissingStatus() throws Exception {
		UserSession leader = signUpAndLogin("schedule-status-missing");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Schedule Status Validation",
			  "localPath": "D:\\\\WE_AI\\\\schedule-status-validation"
			}
			""");

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);
		HttpResponse<String> scheduleCreateResponse = createProjectSchedule(leader.accessToken(), projectId, """
			{
			  "title": "상태 변경 검증 일정",
			  "department": "BACKEND",
			  "startDate": "%s",
			  "endDate": "%s"
			}
			""".formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)));
		long scheduleId = extractLongValue(scheduleCreateResponse.body(), SCHEDULE_ID_PATTERN);

		HttpResponse<String> response = updateProjectScheduleStatus(leader.accessToken(), projectId, scheduleId, """
			{
			  "status": "   "
			}
			""");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"code\":\"PROJECT_400_10\"");
	}

	@Test
	void projectMemberCanFilterSchedulesAndManageProjectTechStacks() throws Exception {
		UserSession leader = signUpAndLogin("filter-tech-leader");
		UserSession member = signUpAndLogin("filter-tech-member");

		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Filter And Tech Stack Project",
			  "localPath": "D:\\\\WE_AI\\\\filter-tech-stack-project"
			}
			""");
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

		HttpResponse<String> backendScheduleResponse = createProjectSchedule(leader.accessToken(), projectId, """
			{
			  "title": "프로젝트 일정 상세 API 구현",
			  "description": "일정 상세 조회 API 개발",
			  "assigneeId": %d,
			  "department": "BACKEND",
			  "startDate": "%s",
			  "endDate": "%s",
			  "priority": "HIGH",
			  "status": "TODO"
			}
			""".formatted(member.userId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)));
		assertThat(backendScheduleResponse.statusCode()).isEqualTo(201);

		HttpResponse<String> frontendScheduleResponse = createProjectSchedule(member.accessToken(), projectId, """
			{
			  "title": "프론트엔드 UI 구성",
			  "description": "메인 화면 UI 작업",
			  "department": "FRONTEND",
			  "startDate": "%s",
			  "endDate": "%s",
			  "priority": "MEDIUM",
			  "status": "DONE"
			}
			""".formatted(LocalDate.now().plusDays(2), LocalDate.now().plusDays(3)));
		assertThat(frontendScheduleResponse.statusCode()).isEqualTo(201);

		HttpResponse<String> filterResponse = getFilteredProjectSchedules(
			leader.accessToken(),
			projectId,
			"?department=BACKEND&status=TODO"
		);
		assertThat(filterResponse.statusCode()).isEqualTo(200);
		assertThat(filterResponse.body()).contains("\"code\":\"PROJECT_SCHEDULE_FILTER_SUCCESS\"");
		assertThat(filterResponse.body()).contains("\"title\":\"프로젝트 일정 상세 API 구현\"");
		assertThat(filterResponse.body()).doesNotContain("\"title\":\"프론트엔드 UI 구성\"");

		HttpResponse<String> createTechStackResponse = createProjectTechStack(leader.accessToken(), projectId, """
			{
			  "name": "Spring Boot",
			  "version": "3.2.5",
			  "category": "BACKEND",
			  "isRequired": true
			}
			""");
		assertThat(createTechStackResponse.statusCode()).isEqualTo(201);
		assertThat(createTechStackResponse.body()).contains("\"code\":\"PROJECT_TECH_STACK_CREATE_SUCCESS\"");
		assertThat(createTechStackResponse.body()).contains("\"name\":\"Spring Boot\"");
		assertThat(createTechStackResponse.body()).contains("\"version\":\"3.2.5\"");
		long techStackId = extractLongValue(createTechStackResponse.body(), TECH_STACK_ID_PATTERN);

		HttpResponse<String> updateTechStackResponse = updateProjectTechStack(leader.accessToken(), projectId, techStackId, """
			{
			  "name": "Spring Boot",
			  "version": "3.3.0",
			  "category": "BACKEND",
			  "isRequired": true
			}
			""");
		assertThat(updateTechStackResponse.statusCode()).isEqualTo(200);
		assertThat(updateTechStackResponse.body()).contains("\"code\":\"PROJECT_TECH_STACK_UPDATE_SUCCESS\"");
		assertThat(updateTechStackResponse.body()).contains("\"version\":\"3.3.0\"");

		HttpResponse<String> deleteTechStackResponse = deleteProjectTechStack(leader.accessToken(), projectId, techStackId);
		assertThat(deleteTechStackResponse.statusCode()).isEqualTo(200);
		assertThat(deleteTechStackResponse.body()).contains("\"code\":\"PROJECT_TECH_STACK_DELETE_SUCCESS\"");
		assertThat(deleteTechStackResponse.body()).contains("\"techStackId\":" + techStackId);

		HttpResponse<String> techStacksAfterDeleteResponse = getProjectTechStacks(leader.accessToken(), projectId);
		assertThat(techStacksAfterDeleteResponse.statusCode()).isEqualTo(200);
		assertThat(techStacksAfterDeleteResponse.body()).doesNotContain("\"name\":\"Spring Boot\"");
	}

	@Test
	void techStackCreateRejectsDuplicateNameAndCategoryWithinProject() throws Exception {
		UserSession leader = signUpAndLogin("tech-duplicate-leader");
		HttpResponse<String> createResponse = createProject(leader.accessToken(), """
			{
			  "projectName": "Duplicate Tech Stack Project",
			  "localPath": "D:\\\\WE_AI\\\\duplicate-tech-stack-project"
			}
			""");
		assertThat(createResponse.statusCode()).isEqualTo(201);

		long projectId = extractLongValue(createResponse.body(), PROJECT_ID_PATTERN);

		HttpResponse<String> firstCreateResponse = createProjectTechStack(leader.accessToken(), projectId, """
			{
			  "name": "Spring Boot",
			  "version": "3.2.5",
			  "category": "BACKEND",
			  "isRequired": true
			}
			""");
		assertThat(firstCreateResponse.statusCode()).isEqualTo(201);

		HttpResponse<String> duplicateCreateResponse = createProjectTechStack(leader.accessToken(), projectId, """
			{
			  "name": "spring boot",
			  "version": "3.3.0",
			  "category": "BACKEND",
			  "isRequired": false
			}
			""");

		assertThat(duplicateCreateResponse.statusCode()).isEqualTo(409);
		assertThat(duplicateCreateResponse.body()).contains("\"code\":\"PROJECT_409_2\"");
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

	private HttpResponse<String> updateProject(String accessToken, long projectId, String requestBody) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d".formatted(port, projectId)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
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

	private HttpResponse<String> getProjectMemberDetail(String accessToken, long projectId, long projectMemberId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/members/%d".formatted(port, projectId, projectMemberId)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> updateProjectMemberRole(
		String accessToken,
		long projectId,
		long projectMemberId,
		String requestBody
	) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/members/%d/role".formatted(port, projectId, projectMemberId)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> updateProjectMemberDepartment(
		String accessToken,
		long projectId,
		long projectMemberId,
		String requestBody
	) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create(
					"http://localhost:%d/api/v1/projects/%d/members/%d/department".formatted(port, projectId, projectMemberId)
				))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
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

	private HttpResponse<String> getFilteredProjectSchedules(String accessToken, long projectId, String queryString) throws Exception {
		String suffix = queryString == null ? "" : queryString;
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/schedules/filter%s".formatted(port, projectId, suffix)))
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

	private HttpResponse<String> getProjectDashboardActivities(String accessToken, long projectId, String queryString) throws Exception {
		String suffix = queryString == null ? "" : queryString;
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/dashboard/activities%s".formatted(port, projectId, suffix)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> getProjectDashboardProgress(String accessToken, long projectId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/dashboard/progress".formatted(port, projectId)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> getProjectDashboardMilestones(String accessToken, long projectId, String queryString) throws Exception {
		String suffix = queryString == null ? "" : queryString;
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/dashboard/milestones%s".formatted(port, projectId, suffix)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> getProjectDashboardDepartments(String accessToken, long projectId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/dashboard/departments".formatted(port, projectId)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> getProjectScheduleDetail(String accessToken, long projectId, long scheduleId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/schedules/%d".formatted(port, projectId, scheduleId)))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> updateProjectSchedule(String accessToken, long projectId, long scheduleId, String requestBody)
		throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/schedules/%d".formatted(port, projectId, scheduleId)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> updateProjectScheduleStatus(
		String accessToken,
		long projectId,
		long scheduleId,
		String requestBody
	) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/schedules/%d/status".formatted(port, projectId, scheduleId)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> deleteProjectSchedule(String accessToken, long projectId, long scheduleId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/schedules/%d".formatted(port, projectId, scheduleId)))
				.header("Authorization", "Bearer " + accessToken)
				.DELETE()
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

	private HttpResponse<String> createProjectTechStack(String accessToken, long projectId, String requestBody) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/tech-stacks".formatted(port, projectId)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private String scheduleBody(String title, String department, String status, LocalDate startDate) {
		return """
			{
			  "title": "%s",
			  "description": "%s description",
			  "department": "%s",
			  "startDate": "%s",
			  "endDate": "%s",
			  "priority": "MEDIUM",
			  "status": "%s"
			}
			""".formatted(title, title, department, startDate, startDate.plusDays(1), status);
	}

	private void insertMilestone(
		long projectId,
		String title,
		String description,
		LocalDate startDate,
		LocalDate endDate,
		String status,
		int progressRate
	) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
			"""
			insert into project_milestones (
			  created_at,
			  updated_at,
			  project_id,
			  title,
			  description,
			  start_date,
			  end_date,
			  status,
			  progress_rate
			) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			now,
			now,
			projectId,
			title,
			description,
			startDate,
			endDate,
			status,
			progressRate
		);
	}

	private HttpResponse<String> updateProjectTechStack(String accessToken, long projectId, long techStackId, String requestBody)
		throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/tech-stacks/%d".formatted(port, projectId, techStackId)))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	private HttpResponse<String> deleteProjectTechStack(String accessToken, long projectId, long techStackId) throws Exception {
		return httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:%d/api/v1/projects/%d/tech-stacks/%d".formatted(port, projectId, techStackId)))
				.header("Authorization", "Bearer " + accessToken)
				.DELETE()
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

	private long extractProjectMemberIdForUser(String responseBody, long userId) {
		Pattern pattern = Pattern.compile("\"projectMemberId\":(\\d+),\"userId\":" + userId);
		return extractLongValue(responseBody, pattern);
	}

	private record UserSession(String username, String email, String accessToken, long userId) {
	}
}
