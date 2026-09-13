package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weai.server.domain.project.domain.BuildTool;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectEnvironmentSetting;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import com.weai.server.domain.project.repository.ProjectEnvironmentSettingRepository;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.request.ActiveSpringProfileUpdateRequest;
import com.weai.server.domain.project.response.ActiveSpringProfileUpdateResponse;
import com.weai.server.domain.project.response.RuntimeEnvironmentResponse;
import com.weai.server.domain.project.response.SpringProfileListResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class ProjectEnvironmentServiceTest {

	private static final String USER_EMAIL = "leader@example.com";
	private static final Long PROJECT_ID = 1L;
	private static final Long USER_ID = 7L;

	private ProjectService projectService;
	private UserService userService;
	private ProjectMemberRepository projectMemberRepository;
	private ProjectEnvironmentSettingRepository projectEnvironmentSettingRepository;
	private Environment environment;
	private ProjectEnvironmentService service;
	private User user;
	private Project project;

	@BeforeEach
	void setUp() {
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
			.projectName("Synaipse Backend")
			.projectCode("ABCDEFG1")
			.status(ProjectStatus.ACTIVE)
			.createdBy(user)
			.build();

		projectService = mock(ProjectService.class);
		userService = mock(UserService.class);
		projectMemberRepository = mock(ProjectMemberRepository.class);
		projectEnvironmentSettingRepository = mock(ProjectEnvironmentSettingRepository.class);
		environment = mock(Environment.class);
		service = new ProjectEnvironmentService(
			projectService,
			userService,
			projectMemberRepository,
			projectEnvironmentSettingRepository,
			environment
		);

		when(userService.getUserEntityByEmail(USER_EMAIL)).thenReturn(user);
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID)).thenReturn(project);
		when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
		when(environment.getDefaultProfiles()).thenReturn(new String[] {"default"});
	}

	@Test
	void getRuntimeEnvironmentReturnsServerRuntimeInfo() {
		RuntimeEnvironmentResponse response = service.getRuntimeEnvironment(USER_EMAIL, PROJECT_ID);

		assertThat(response.projectId()).isEqualTo(PROJECT_ID);
		assertThat(response.projectName()).isEqualTo("Synaipse Backend");
		assertThat(response.javaVersion()).isNotBlank();
		assertThat(response.osName()).isNotBlank();
		assertThat(response.activeProfiles()).containsExactly("test");
		assertThat(response.defaultProfiles()).containsExactly("default");
		assertThat(response.memory().usedMemory()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void getRuntimeEnvironmentDoesNotExposeSensitiveEnvironmentVariables() {
		RuntimeEnvironmentResponse response = service.getRuntimeEnvironment(USER_EMAIL, PROJECT_ID);

		assertThat(response.toString().toLowerCase())
			.doesNotContain("password")
			.doesNotContain("secret")
			.doesNotContain("token");
	}

	@Test
	void getSpringProfilesUsesStoredActiveProfile() {
		when(projectEnvironmentSettingRepository.findByProject_Id(PROJECT_ID))
			.thenReturn(Optional.of(setting("prod")));

		SpringProfileListResponse response = service.getSpringProfiles(USER_EMAIL, PROJECT_ID);

		assertThat(response.activeProfile()).isEqualTo("prod");
		assertThat(response.profiles()).hasSize(4);
		assertThat(response.profiles())
			.filteredOn(SpringProfileListResponse.SpringProfileResponse::active)
			.extracting(SpringProfileListResponse.SpringProfileResponse::profile)
			.containsExactly("prod");
	}

	@Test
	void getSpringProfilesFallsBackToEnvironmentProfileWhenSettingDoesNotExist() {
		when(projectEnvironmentSettingRepository.findByProject_Id(PROJECT_ID)).thenReturn(Optional.empty());

		SpringProfileListResponse response = service.getSpringProfiles(USER_EMAIL, PROJECT_ID);

		assertThat(response.activeProfile()).isEqualTo("test");
		assertThat(response.profiles()).extracting(SpringProfileListResponse.SpringProfileResponse::profile)
			.containsExactly("local", "dev", "test", "prod");
	}

	@Test
	void updateActiveProfileUpdatesStoredSetting() {
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(projectMember(ProjectMemberRole.LEADER)));
		ProjectEnvironmentSetting setting = setting("dev");
		when(projectEnvironmentSettingRepository.findByProject_Id(PROJECT_ID)).thenReturn(Optional.of(setting));
		when(projectEnvironmentSettingRepository.save(setting)).thenReturn(setting);

		ActiveSpringProfileUpdateResponse response = service.updateActiveProfile(
			USER_EMAIL,
			PROJECT_ID,
			new ActiveSpringProfileUpdateRequest("prod")
		);

		assertThat(response.previousProfile()).isEqualTo("dev");
		assertThat(response.activeProfile()).isEqualTo("prod");
		assertThat(response.changedBy()).isEqualTo(USER_ID);
		assertThat(response.appliedImmediately()).isFalse();
		assertThat(response.requiresRestart()).isTrue();
		verify(projectEnvironmentSettingRepository).save(setting);
	}

	@Test
	void updateActiveProfileCreatesSettingWhenMissing() {
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(projectMember(ProjectMemberRole.LEADER)));
		when(projectEnvironmentSettingRepository.findByProject_Id(PROJECT_ID)).thenReturn(Optional.empty());
		when(projectEnvironmentSettingRepository.save(org.mockito.ArgumentMatchers.any(ProjectEnvironmentSetting.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		ActiveSpringProfileUpdateResponse response = service.updateActiveProfile(
			USER_EMAIL,
			PROJECT_ID,
			new ActiveSpringProfileUpdateRequest("local")
		);

		assertThat(response.previousProfile()).isEqualTo("test");
		assertThat(response.activeProfile()).isEqualTo("local");
	}

	@Test
	void updateActiveProfileFailsWhenProfileIsMissing() {
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(projectMember(ProjectMemberRole.LEADER)));

		assertThatThrownBy(() -> service.updateActiveProfile(USER_EMAIL, PROJECT_ID, new ActiveSpringProfileUpdateRequest(" ")))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SPRING_PROFILE_REQUIRED));
	}

	@Test
	void updateActiveProfileFailsWhenProfileIsInvalid() {
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(projectMember(ProjectMemberRole.LEADER)));

		assertThatThrownBy(() -> service.updateActiveProfile(USER_EMAIL, PROJECT_ID, new ActiveSpringProfileUpdateRequest("stage")))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SPRING_PROFILE));
	}

	@Test
	void updateActiveProfileFailsWhenRequesterIsMember() {
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(projectMember(ProjectMemberRole.MEMBER)));

		assertThatThrownBy(() -> service.updateActiveProfile(USER_EMAIL, PROJECT_ID, new ActiveSpringProfileUpdateRequest("prod")))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_LEADER_ONLY));
	}

	@Test
	void projectAccessFailureIsPropagated() {
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID))
			.thenThrow(new ApiException(ErrorCode.PROJECT_ACCESS_DENIED));

		assertThatThrownBy(() -> service.getSpringProfiles(USER_EMAIL, PROJECT_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED));
	}

	@Test
	void projectNotFoundFailureIsPropagated() {
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID))
			.thenThrow(new ApiException(ErrorCode.PROJECT_NOT_FOUND));

		assertThatThrownBy(() -> service.getRuntimeEnvironment(USER_EMAIL, PROJECT_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_NOT_FOUND));
	}

	private ProjectEnvironmentSetting setting(String activeProfile) {
		return ProjectEnvironmentSetting.builder()
			.environmentSettingId(10L)
			.project(project)
			.activeProfile(activeProfile)
			.buildTool(BuildTool.GRADLE)
			.javaVersion("17")
			.changedBy(user)
			.changedAt(LocalDateTime.now())
			.build();
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
}
