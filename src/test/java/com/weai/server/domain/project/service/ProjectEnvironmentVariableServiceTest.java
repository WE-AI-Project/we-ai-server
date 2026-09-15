package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectEnvironmentVariable;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import com.weai.server.domain.project.repository.ProjectEnvironmentVariableRepository;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.request.ProjectEnvironmentVariableCreateRequest;
import com.weai.server.domain.project.request.ProjectEnvironmentVariableUpdateRequest;
import com.weai.server.domain.project.response.ProjectEnvironmentVariableListResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectEnvironmentVariableServiceTest {

	private static final String USER_EMAIL = "leader@example.com";
	private static final Long PROJECT_ID = 1L;
	private static final Long USER_ID = 7L;
	private static final Long VARIABLE_ID = 10L;

	private ProjectService projectService;
	private UserService userService;
	private ProjectMemberRepository projectMemberRepository;
	private ProjectEnvironmentVariableRepository projectEnvironmentVariableRepository;
	private ProjectEnvironmentValueCipher environmentValueCipher;
	private ProjectEnvironmentVariableService service;
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
			.projectName("Project")
			.projectCode("ABCDEFG1")
			.status(ProjectStatus.ACTIVE)
			.createdBy(user)
			.build();

		projectService = mock(ProjectService.class);
		userService = mock(UserService.class);
		projectMemberRepository = mock(ProjectMemberRepository.class);
		projectEnvironmentVariableRepository = mock(ProjectEnvironmentVariableRepository.class);
		environmentValueCipher = new ProjectEnvironmentValueCipher(
			Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes())
		);
		service = new ProjectEnvironmentVariableService(
			projectService,
			userService,
			projectMemberRepository,
			projectEnvironmentVariableRepository,
			environmentValueCipher
		);

		when(userService.getUserEntityByEmail(USER_EMAIL)).thenReturn(user);
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID)).thenReturn(project);
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(projectMember(ProjectMemberRole.LEADER)));
	}

	@Test
	void listMasksSecretValueWithoutDecryptingItForTheResponse() {
		ProjectEnvironmentVariable secret = variable("OPENAI_API_KEY", "dev", "actual-secret", true);
		when(projectEnvironmentVariableRepository.findActiveVariables(PROJECT_ID, "dev", null))
			.thenReturn(List.of(secret));

		ProjectEnvironmentVariableListResponse response = service.getEnvironmentVariablesForView(
			USER_EMAIL, PROJECT_ID, "dev", null
		);

		assertThat(response.variables()).singleElement().satisfies(variable -> {
			assertThat(variable.value()).isNull();
			assertThat(variable.maskedValue()).isEqualTo("********");
			assertThat(variable.toString()).doesNotContain("actual-secret");
		});
	}

	@Test
	void createEncryptsValueBeforeSaving() {
		when(projectEnvironmentVariableRepository.findByProject_IdAndProfileAndVariableKeyAndDeletedAtIsNull(
			PROJECT_ID, "dev", "OPENAI_API_KEY"
		)).thenReturn(Optional.empty());
		when(projectEnvironmentVariableRepository.save(any(ProjectEnvironmentVariable.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		service.createEnvironmentVariable(
			USER_EMAIL,
			PROJECT_ID,
			new ProjectEnvironmentVariableCreateRequest("OPENAI_API_KEY", "actual-secret", "DEV", true, "API key", true)
		);

		org.mockito.ArgumentCaptor<ProjectEnvironmentVariable> captor = org.mockito.ArgumentCaptor.forClass(
			ProjectEnvironmentVariable.class
		);
		verify(projectEnvironmentVariableRepository).save(captor.capture());
		assertThat(captor.getValue().getEncryptedValue()).isNotEqualTo("actual-secret");
		assertThat(environmentValueCipher.decrypt(captor.getValue().getEncryptedValue())).isEqualTo("actual-secret");
	}

	@Test
	void createRejectsDuplicateProjectProfileAndKey() {
		when(projectEnvironmentVariableRepository.findByProject_IdAndProfileAndVariableKeyAndDeletedAtIsNull(
			PROJECT_ID, "dev", "OPENAI_API_KEY"
		)).thenReturn(Optional.of(variable("OPENAI_API_KEY", "dev", "old", true)));

		assertThatThrownBy(() -> service.createEnvironmentVariable(
			USER_EMAIL,
			PROJECT_ID,
			new ProjectEnvironmentVariableCreateRequest("OPENAI_API_KEY", "actual-secret", "dev", true, null, true)
		)).isInstanceOfSatisfying(ApiException.class, exception ->
			assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENVIRONMENT_VARIABLE_ALREADY_EXISTS));
	}

	@Test
	void createRejectsInvalidVariableKey() {
		assertThatThrownBy(() -> service.createEnvironmentVariable(
			USER_EMAIL,
			PROJECT_ID,
			new ProjectEnvironmentVariableCreateRequest("openai-key", "actual-secret", "dev", true, null, true)
		)).isInstanceOfSatisfying(ApiException.class, exception ->
			assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ENVIRONMENT_VARIABLE_KEY));
	}

	@Test
	void updateWithoutValueKeepsExistingEncryptedValue() {
		ProjectEnvironmentVariable variable = variable("OPENAI_API_KEY", "dev", "old-secret", true);
		when(projectEnvironmentVariableRepository.findByEnvironmentVariableIdAndProject_IdAndDeletedAtIsNull(VARIABLE_ID, PROJECT_ID))
			.thenReturn(Optional.of(variable));
		when(projectEnvironmentVariableRepository.findByProject_IdAndProfileAndVariableKeyAndDeletedAtIsNull(
			PROJECT_ID, "prod", "OPENAI_API_KEY"
		)).thenReturn(Optional.of(variable));
		String before = variable.getEncryptedValue();

		service.updateEnvironmentVariable(
			USER_EMAIL, PROJECT_ID, VARIABLE_ID,
			new ProjectEnvironmentVariableUpdateRequest(null, "prod", null, "production", null)
		);

		assertThat(variable.getEncryptedValue()).isEqualTo(before);
		assertThat(variable.getProfile()).isEqualTo("prod");
		assertThat(variable.getDescription()).isEqualTo("production");
	}

	@Test
	void deleteSoftDeletesVariable() {
		ProjectEnvironmentVariable variable = variable("OPENAI_API_KEY", "dev", "actual-secret", true);
		when(projectEnvironmentVariableRepository.findByEnvironmentVariableIdAndProject_IdAndDeletedAtIsNull(VARIABLE_ID, PROJECT_ID))
			.thenReturn(Optional.of(variable));

		service.deleteEnvironmentVariable(USER_EMAIL, PROJECT_ID, VARIABLE_ID);

		assertThat(variable.getDeletedAt()).isNotNull();
	}

	@Test
	void memberCannotCreateVariable() {
		when(projectMemberRepository.findByProject_IdAndUser_IdAndStatus(PROJECT_ID, USER_ID, ProjectMemberStatus.ACTIVE))
			.thenReturn(Optional.of(projectMember(ProjectMemberRole.MEMBER)));

		assertThatThrownBy(() -> service.createEnvironmentVariable(
			USER_EMAIL,
			PROJECT_ID,
			new ProjectEnvironmentVariableCreateRequest("OPENAI_API_KEY", "actual-secret", "dev", true, null, true)
		)).isInstanceOfSatisfying(ApiException.class, exception ->
			assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_LEADER_ONLY));
	}

	@Test
	void executionLookupDecryptsOnlyEnabledVariablesInProfile() {
		ProjectEnvironmentVariable enabled = variable("OPENAI_API_KEY", "dev", "actual-secret", true);
		when(projectEnvironmentVariableRepository
			.findByProject_IdAndProfileAndEnabledTrueAndDeletedAtIsNullOrderByVariableKeyAsc(PROJECT_ID, "dev"))
			.thenReturn(List.of(enabled));

		Map<String, String> values = service.getEnvironmentVariables(PROJECT_ID, "DEV");

		assertThat(values).containsEntry("OPENAI_API_KEY", "actual-secret");
	}

	private ProjectEnvironmentVariable variable(String key, String profile, String value, boolean secret) {
		return ProjectEnvironmentVariable.builder()
			.environmentVariableId(VARIABLE_ID)
			.project(project)
			.profile(profile)
			.variableKey(key)
			.encryptedValue(environmentValueCipher.encrypt(value))
			.secret(secret)
			.description(null)
			.enabled(true)
			.createdBy(user)
			.updatedBy(user)
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
