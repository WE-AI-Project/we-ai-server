package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weai.server.domain.project.domain.BuildRun;
import com.weai.server.domain.project.domain.BuildRunStatus;
import com.weai.server.domain.project.domain.BuildTool;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import com.weai.server.domain.project.repository.BuildRunRepository;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.request.BuildTaskRunRequest;
import com.weai.server.domain.project.response.BuildRunHistoryResponse;
import com.weai.server.domain.project.response.BuildRunResultResponse;
import com.weai.server.domain.project.response.BuildTaskRunResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class ProjectBuildRunServiceTest {

	private static final String USER_EMAIL = "leader@example.com";
	private static final Long PROJECT_ID = 1L;
	private static final Long USER_ID = 7L;
	private static final Long BUILD_RUN_ID = 11L;

	@TempDir
	Path projectRoot;

	private ProjectService projectService;
	private UserService userService;
	private ProjectMemberRepository projectMemberRepository;
	private BuildRunRepository buildRunRepository;
	private BuildRunExecutionWorker buildRunExecutionWorker;
	private ProjectEnvironmentService projectEnvironmentService;
	private ProjectBuildRunService service;
	private User user;
	private Project project;

	@BeforeEach
	void setUp() throws IOException {
		user = User.builder()
			.id(USER_ID)
			.username("leader")
			.password("password")
			.name("Leader")
			.email(USER_EMAIL)
			.role(UserRole.USER)
			.build();
		project = Project.builder()
			.id(PROJECT_ID)
			.projectName("Project")
			.projectCode("ABCDEFG1")
			.localPath(projectRoot.toString())
			.status(ProjectStatus.ACTIVE)
			.createdBy(user)
			.build();

		projectService = mock(ProjectService.class);
		userService = mock(UserService.class);
		projectMemberRepository = mock(ProjectMemberRepository.class);
		buildRunRepository = mock(BuildRunRepository.class);
		buildRunExecutionWorker = mock(BuildRunExecutionWorker.class);
		projectEnvironmentService = mock(ProjectEnvironmentService.class);
		TaskExecutor directExecutor = Runnable::run;

		service = new ProjectBuildRunService(
			projectService,
			userService,
			projectMemberRepository,
			buildRunRepository,
			buildRunExecutionWorker,
			projectEnvironmentService,
			directExecutor
		);

		when(userService.getUserEntityByEmail(USER_EMAIL)).thenReturn(user);
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID)).thenReturn(project);
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(projectMember(ProjectMemberRole.LEADER)));
		when(buildRunRepository.existsByProject_IdAndStatusIn(eq(PROJECT_ID), any())).thenReturn(false);
		when(buildRunRepository.saveAndFlush(any(BuildRun.class))).thenReturn(runningBuildRun());

		createGradleWrapper(projectRoot);
	}

	@Test
	void runBuildTaskStartsAsyncBuildRun() {
		BuildTaskRunResponse response = service.runBuildTask(
			USER_EMAIL,
			PROJECT_ID,
			new BuildTaskRunRequest("build", "dev")
		);

		assertThat(response.buildRunId()).isEqualTo(BUILD_RUN_ID);
		assertThat(response.projectId()).isEqualTo(PROJECT_ID);
		assertThat(response.taskName()).isEqualTo("build");
		assertThat(response.buildTool()).isEqualTo(BuildTool.GRADLE);
		assertThat(response.profile()).isEqualTo("dev");
		assertThat(response.status()).isEqualTo(BuildRunStatus.RUNNING);
		assertThat(response.command()).contains("build", "-Dspring.profiles.active=dev");
		verify(buildRunExecutionWorker).execute(eq(BUILD_RUN_ID), any(BuildCommand.class));
	}

	@Test
	void runBuildTaskFailsWhenTaskNameIsMissing() {
		assertThatThrownBy(() -> service.runBuildTask(USER_EMAIL, PROJECT_ID, new BuildTaskRunRequest(" ", null)))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BUILD_TASK_NAME_REQUIRED));
	}

	@Test
	void runBuildTaskFailsWhenTaskNameIsNotAllowed() {
		assertThatThrownBy(() -> service.runBuildTask(USER_EMAIL, PROJECT_ID, new BuildTaskRunRequest("bootRun", null)))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_BUILD_TASK_NAME));
	}

	@Test
	void runBuildTaskFailsWhenBuildIsAlreadyRunning() {
		when(buildRunRepository.existsByProject_IdAndStatusIn(eq(PROJECT_ID), any())).thenReturn(true);

		assertThatThrownBy(() -> service.runBuildTask(USER_EMAIL, PROJECT_ID, new BuildTaskRunRequest("test", null)))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BUILD_ALREADY_RUNNING));
	}

	@Test
	void runBuildTaskFailsWhenRequesterIsNotLeader() {
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(projectMember(ProjectMemberRole.MEMBER)));

		assertThatThrownBy(() -> service.runBuildTask(USER_EMAIL, PROJECT_ID, new BuildTaskRunRequest("build", null)))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_LEADER_ONLY));
	}

	@Test
	void runBuildTaskFailsWhenGradleWrapperIsMissing() throws IOException {
		Files.delete(projectRoot.resolve(isWindows() ? "gradlew.bat" : "gradlew"));

		assertThatThrownBy(() -> service.runBuildTask(USER_EMAIL, PROJECT_ID, new BuildTaskRunRequest("build", null)))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GRADLE_WRAPPER_NOT_FOUND));
	}

	@Test
	void getBuildRunResultReturnsBuildRun() {
		when(buildRunRepository.findByBuildRunIdAndProject_Id(BUILD_RUN_ID, PROJECT_ID))
			.thenReturn(Optional.of(successBuildRun()));

		BuildRunResultResponse response = service.getBuildRunResult(USER_EMAIL, PROJECT_ID, BUILD_RUN_ID);

		assertThat(response.buildRunId()).isEqualTo(BUILD_RUN_ID);
		assertThat(response.status()).isEqualTo(BuildRunStatus.SUCCESS);
		assertThat(response.exitCode()).isZero();
		assertThat(response.requesterName()).isEqualTo("Leader");
	}

	@Test
	void getBuildRunResultFailsWhenBuildRunDoesNotExist() {
		when(buildRunRepository.findByBuildRunIdAndProject_Id(BUILD_RUN_ID, PROJECT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getBuildRunResult(USER_EMAIL, PROJECT_ID, BUILD_RUN_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BUILD_RUN_NOT_FOUND));
	}

	@Test
	@SuppressWarnings("unchecked")
	void getBuildRunHistoryReturnsPagedRuns() {
		when(buildRunRepository.findAll(any(Specification.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(successBuildRun())));
		when(buildRunRepository.count(any(Specification.class))).thenReturn(1L, 1L, 0L, 0L);

		BuildRunHistoryResponse response = service.getBuildRunHistory(
			USER_EMAIL,
			PROJECT_ID,
			0,
			20,
			null,
			null,
			null
		);

		assertThat(response.totalCount()).isEqualTo(1);
		assertThat(response.successCount()).isEqualTo(1);
		assertThat(response.runs()).hasSize(1);
		assertThat(response.runs().get(0).buildRunId()).isEqualTo(BUILD_RUN_ID);
	}

	@Test
	void getBuildRunHistoryFailsWhenStatusIsInvalid() {
		assertThatThrownBy(() -> service.getBuildRunHistory(USER_EMAIL, PROJECT_ID, 0, 20, "DONE", null, null))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_BUILD_RUN_STATUS));
	}

	@Test
	void projectAccessFailureIsPropagated() {
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID))
			.thenThrow(new ApiException(ErrorCode.PROJECT_ACCESS_DENIED));

		assertThatThrownBy(() -> service.getBuildRunHistory(USER_EMAIL, PROJECT_ID, 0, 20, null, null, null))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED));
	}

	private ProjectMember projectMember(ProjectMemberRole role) {
		return ProjectMember.builder()
			.id(1L)
			.project(project)
			.user(user)
			.role(role)
			.department(ProjectDepartment.BACKEND)
			.status(ProjectMemberStatus.ACTIVE)
			.joinedAt(LocalDateTime.now())
			.build();
	}

	private BuildRun runningBuildRun() {
		return BuildRun.builder()
			.buildRunId(BUILD_RUN_ID)
			.project(project)
			.requester(user)
			.taskName("build")
			.buildTool(BuildTool.GRADLE)
			.profile("dev")
			.status(BuildRunStatus.RUNNING)
			.command((isWindows() ? "gradlew.bat" : "./gradlew") + " build -Dspring.profiles.active=dev")
			.startedAt(LocalDateTime.now())
			.output("")
			.errorOutput("")
			.build();
	}

	private BuildRun successBuildRun() {
		return BuildRun.builder()
			.buildRunId(BUILD_RUN_ID)
			.project(project)
			.requester(user)
			.taskName("build")
			.buildTool(BuildTool.GRADLE)
			.profile("dev")
			.status(BuildRunStatus.SUCCESS)
			.command((isWindows() ? "gradlew.bat" : "./gradlew") + " build")
			.exitCode(0)
			.output("BUILD SUCCESSFUL")
			.errorOutput(null)
			.startedAt(LocalDateTime.now().minusSeconds(2))
			.finishedAt(LocalDateTime.now())
			.durationMs(2000L)
			.build();
	}

	private void createGradleWrapper(Path root) throws IOException {
		Path wrapper = root.resolve(isWindows() ? "gradlew.bat" : "gradlew");
		Files.writeString(wrapper, isWindows() ? "@echo off\r\necho ok\r\n" : "#!/bin/sh\necho ok\n");
	}

	private boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
	}
}
