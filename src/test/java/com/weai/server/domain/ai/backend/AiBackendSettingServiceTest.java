package com.weai.server.domain.ai.backend;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.service.ProjectEnvironmentValueCipher;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AiBackendSettingServiceTest {

	private static final String USER_EMAIL = "member@example.com";
	private static final Long USER_ID = 7L;

	private UserService userService;
	private ProjectService projectService;
	private ProjectMemberRepository projectMemberRepository;
	private UserAiBackendSettingRepository userAiBackendSettingRepository;
	private ProjectAiBackendSettingRepository projectAiBackendSettingRepository;
	private ProjectEnvironmentValueCipher cipher;
	private AiBackendConnectionTester connectionTester;
	private AiBackendSettingService service;
	private User user;

	@BeforeEach
	void setUp() {
		user = User.builder()
			.id(USER_ID)
			.username("member")
			.password("password")
			.name("Member")
			.email(USER_EMAIL)
			.role(UserRole.USER)
			.build();

		userService = mock(UserService.class);
		projectService = mock(ProjectService.class);
		projectMemberRepository = mock(ProjectMemberRepository.class);
		userAiBackendSettingRepository = mock(UserAiBackendSettingRepository.class);
		projectAiBackendSettingRepository = mock(ProjectAiBackendSettingRepository.class);
		cipher = mock(ProjectEnvironmentValueCipher.class);
		connectionTester = mock(AiBackendConnectionTester.class);
		service = new AiBackendSettingService(
			userService,
			projectService,
			projectMemberRepository,
			userAiBackendSettingRepository,
			projectAiBackendSettingRepository,
			cipher,
			connectionTester,
			new PrivateNetworkGuard()
		);

		when(userService.getUserEntityByEmail(USER_EMAIL)).thenReturn(user);
		when(userAiBackendSettingRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
		when(userAiBackendSettingRepository.save(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"http://127.0.0.1:11434",
		"http://localhost:11434",
		"http://10.0.0.5:11434",
		"http://mysql:3306",
		"http://100.127.105.105:11434"
	})
	void rejectsEnablingWithAPrivateOrInternalBaseUrl(String baseUrl) {
		AiBackendSettingUpdateRequest request = new AiBackendSettingUpdateRequest(
			true, "OLLAMA_NATIVE", baseUrl, null, null, null
		);

		assertThatThrownBy(() -> service.updatePersonal(USER_EMAIL, request))
			.isInstanceOf(ApiException.class)
			.extracting(exception -> ((ApiException) exception).getErrorCode())
			.isEqualTo(ErrorCode.AI_BACKEND_BASE_URL_NOT_ALLOWED);
	}

	@Test
	void allowsDisablingEvenWithAPreviouslyPrivateBaseUrlStored() {
		AiBackendSettingUpdateRequest request = new AiBackendSettingUpdateRequest(
			false, "OLLAMA_NATIVE", "http://127.0.0.1:11434", null, null, null
		);

		service.updatePersonal(USER_EMAIL, request);
	}
}
