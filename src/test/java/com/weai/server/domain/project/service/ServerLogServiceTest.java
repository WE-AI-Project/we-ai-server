package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import com.weai.server.domain.project.domain.ServerLog;
import com.weai.server.domain.project.domain.ServerLogLevel;
import com.weai.server.domain.project.domain.ServerLogSource;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ServerLogRepository;
import com.weai.server.domain.project.response.ServerLogClearResponse;
import com.weai.server.domain.project.response.ServerLogListResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ServerLogServiceTest {

	private static final Long PROJECT_ID = 1L;
	private static final Long USER_ID = 7L;
	private static final String USER_EMAIL = "leader@example.com";

	private ProjectService projectService;
	private UserService userService;
	private ProjectMemberRepository projectMemberRepository;
	private ServerLogRepository serverLogRepository;
	private ServerLogService serverLogService;
	private Project project;
	private User user;

	@BeforeEach
	void setUp() {
		projectService = mock(ProjectService.class);
		userService = mock(UserService.class);
		projectMemberRepository = mock(ProjectMemberRepository.class);
		serverLogRepository = mock(ServerLogRepository.class);
		serverLogService = new ServerLogService(
			projectService,
			userService,
			projectMemberRepository,
			serverLogRepository,
			mock(ServerLogStreamService.class)
		);
		user = User.builder()
			.id(USER_ID).username("leader").password("password").name("Leader").email(USER_EMAIL).role(UserRole.USER).build();
		project = Project.builder()
			.id(PROJECT_ID).projectName("Project").projectCode("ABCDEFG1").status(ProjectStatus.ACTIVE).createdBy(user).build();
		when(userService.getUserEntityByEmail(USER_EMAIL)).thenReturn(user);
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID)).thenReturn(project);
	}

	@Test
	void getLogsReturnsLogsAndLevelCounts() {
		ServerLog serverLog = ServerLog.create(
			project, ServerLogLevel.ERROR, ServerLogSource.SPRING_BOOT, "Database failed", "main", "DatabaseConfig", "trace-1"
		);
		when(serverLogRepository.findByProject_IdAndDeletedAtIsNull(eq(PROJECT_ID), any()))
			.thenReturn(new PageImpl<>(List.of(serverLog), PageRequest.of(0, 100), 1));
		when(serverLogRepository.countByProject_IdAndLevelAndDeletedAtIsNull(PROJECT_ID, ServerLogLevel.ERROR)).thenReturn(1L);
		when(serverLogRepository.countByProject_IdAndLevelAndDeletedAtIsNull(PROJECT_ID, ServerLogLevel.WARN)).thenReturn(2L);
		when(serverLogRepository.countByProject_IdAndLevelAndDeletedAtIsNull(PROJECT_ID, ServerLogLevel.INFO)).thenReturn(3L);

		ServerLogListResponse response = serverLogService.getLogs(USER_EMAIL, PROJECT_ID, null, null);

		assertThat(response.totalCount()).isEqualTo(1);
		assertThat(response.errorCount()).isEqualTo(1);
		assertThat(response.warnCount()).isEqualTo(2);
		assertThat(response.infoCount()).isEqualTo(3);
		assertThat(response.logs()).hasSize(1);
		assertThat(response.logs().get(0).message()).isEqualTo("Database failed");
	}

	@Test
	void searchLogsRejectsInvalidFilterValues() {
		assertThatThrownBy(() -> serverLogService.searchLogs(
			USER_EMAIL, PROJECT_ID, null, "UNKNOWN", null, null, null, 0, 100
		)).isInstanceOfSatisfying(ApiException.class, exception ->
			assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SERVER_LOG_LEVEL));
		assertThatThrownBy(() -> serverLogService.searchLogs(
			USER_EMAIL, PROJECT_ID, null, null, null,
			java.time.LocalDateTime.of(2026, 9, 9, 0, 0),
			java.time.LocalDateTime.of(2026, 9, 8, 0, 0),
			0, 100
		)).isInstanceOfSatisfying(ApiException.class, exception ->
			assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_LOG_DATE_RANGE));
	}

	@Test
	void clearLogsSoftDeletesLogsForProjectLeader() {
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(member(ProjectMemberRole.LEADER)));
		when(serverLogRepository.softDeleteAllByProjectId(eq(PROJECT_ID), any())).thenReturn(4);

		ServerLogClearResponse response = serverLogService.clearLogs(USER_EMAIL, PROJECT_ID);

		assertThat(response.projectId()).isEqualTo(PROJECT_ID);
		assertThat(response.clearedCount()).isEqualTo(4);
		assertThat(response.clearedAt()).isNotNull();
	}

	@Test
	void clearLogsRejectsNonLeader() {
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(member(ProjectMemberRole.MEMBER)));

		assertThatThrownBy(() -> serverLogService.clearLogs(USER_EMAIL, PROJECT_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_LEADER_ONLY));
	}

	private ProjectMember member(ProjectMemberRole role) {
		return ProjectMember.builder()
			.project(project).user(user).role(role).department(ProjectDepartment.BACKEND).status(ProjectMemberStatus.ACTIVE)
			.joinedAt(java.time.LocalDateTime.now()).build();
	}
}
