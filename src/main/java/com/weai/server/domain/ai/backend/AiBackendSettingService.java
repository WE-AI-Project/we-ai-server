package com.weai.server.domain.ai.backend;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.service.ProjectEnvironmentValueCipher;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiBackendSettingService {

	private final UserService userService;
	private final ProjectService projectService;
	private final ProjectMemberRepository projectMemberRepository;
	private final UserAiBackendSettingRepository userAiBackendSettingRepository;
	private final ProjectAiBackendSettingRepository projectAiBackendSettingRepository;
	private final ProjectEnvironmentValueCipher cipher;
	private final AiBackendConnectionTester connectionTester;
	private final PrivateNetworkGuard privateNetworkGuard;

	// ── 개인 ──

	public AiBackendSettingResponse getPersonal(String userEmail) {
		User user = userService.getUserEntityByEmail(userEmail);
		return userAiBackendSettingRepository.findByUser_Id(user.getId())
			.map(AiBackendSettingResponse::from)
			.orElseGet(() -> AiBackendSettingResponse.from(UserAiBackendSetting.createDefault(user)));
	}

	@Transactional
	public AiBackendSettingResponse updatePersonal(String userEmail, AiBackendSettingUpdateRequest rawRequest) {
		AiBackendSettingUpdateRequest request = requireRequest(rawRequest);
		User user = userService.getUserEntityByEmail(userEmail);
		UserAiBackendSetting setting = userAiBackendSettingRepository.findByUser_Id(user.getId())
			.orElseGet(() -> UserAiBackendSetting.createDefault(user));

		boolean enabled = request.enabled() != null && request.enabled();
		AiBackendDialect dialect = AiBackendDialect.from(request.dialect());
		String baseUrl = normalizeBaseUrl(request.baseUrl());
		validateEnabled(enabled, baseUrl);

		setting.applySettings(enabled, dialect, baseUrl, trimToNull(request.modelName()), normalizeHealthPath(request.healthPath()));
		if (StringUtils.hasText(request.apiKey())) {
			setting.replaceApiKey(encrypt(request.apiKey()));
		}

		return AiBackendSettingResponse.from(userAiBackendSettingRepository.save(setting));
	}

	@Transactional
	public void deletePersonalApiKey(String userEmail) {
		User user = userService.getUserEntityByEmail(userEmail);
		userAiBackendSettingRepository.findByUser_Id(user.getId()).ifPresent(UserAiBackendSetting::clearApiKey);
	}

	// ── 프로젝트 공용 ──

	public AiBackendSettingResponse getProject(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		return projectAiBackendSettingRepository.findByProject_Id(projectId)
			.map(AiBackendSettingResponse::from)
			.orElseGet(() -> AiBackendSettingResponse.from(ProjectAiBackendSetting.createDefault(project, user)));
	}

	@Transactional
	public AiBackendSettingResponse updateProject(String userEmail, Long projectId, AiBackendSettingUpdateRequest rawRequest) {
		AiBackendSettingUpdateRequest request = requireRequest(rawRequest);
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		validateLeader(projectId, user.getId());

		ProjectAiBackendSetting setting = projectAiBackendSettingRepository.findByProject_Id(projectId)
			.orElseGet(() -> ProjectAiBackendSetting.createDefault(project, user));

		boolean enabled = request.enabled() != null && request.enabled();
		AiBackendDialect dialect = AiBackendDialect.from(request.dialect());
		String baseUrl = normalizeBaseUrl(request.baseUrl());
		validateEnabled(enabled, baseUrl);

		setting.applySettings(enabled, dialect, baseUrl, trimToNull(request.modelName()), normalizeHealthPath(request.healthPath()), user);
		if (StringUtils.hasText(request.apiKey())) {
			setting.replaceApiKey(encrypt(request.apiKey()), user);
		}

		return AiBackendSettingResponse.from(projectAiBackendSettingRepository.save(setting));
	}

	@Transactional
	public void deleteProjectApiKey(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		validateLeader(projectId, user.getId());
		projectAiBackendSettingRepository.findByProject_Id(projectId)
			.ifPresent(setting -> setting.clearApiKey(user));
	}

	// ── 연결 테스트 (저장 전, 서버가 대신 호출) ──

	public AiBackendConnectionTestResponse testConnection(AiBackendConnectionTestRequest request) {
		AiBackendDialect.from(request.dialect());
		return connectionTester.test(request.baseUrl(), request.healthPath(), request.apiKey());
	}

	private void validateLeader(Long projectId, Long userId) {
		ProjectMember member = projectMemberRepository
			.findByProject_IdAndUser_IdAndStatus(projectId, userId, ProjectMemberStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_ACCESS_DENIED));
		if (!member.isLeader()) {
			throw new ApiException(ErrorCode.PROJECT_LEADER_ONLY);
		}
	}

	private AiBackendSettingUpdateRequest requireRequest(AiBackendSettingUpdateRequest request) {
		if (request == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "Request body is required.");
		}
		return request;
	}

	// enabled=true로 저장되는 순간 실제 채팅 트래픽이 이 baseUrl로 나가므로(AiBackendResolver ->
	// DynamicAiChatModelFactory), 사설/내부 네트워크 주소는 저장 자체를 막는다. 그렇지 않으면 RAG로 끌어온
	// 프로젝트 문서 컨텍스트와 질문이 그대로 내부 서비스나 이 서버가 접근 가능한 사설망으로 전송될 수 있다.
	private void validateEnabled(boolean enabled, String baseUrl) {
		if (!enabled) {
			return;
		}
		if (!StringUtils.hasText(baseUrl)) {
			throw new ApiException(ErrorCode.AI_BACKEND_BASE_URL_REQUIRED);
		}
		try {
			privateNetworkGuard.assertPubliclyRoutable(baseUrl);
		} catch (IllegalArgumentException invalidTarget) {
			throw new ApiException(ErrorCode.AI_BACKEND_BASE_URL_NOT_ALLOWED);
		}
	}

	private String normalizeBaseUrl(String baseUrl) {
		String trimmed = trimToNull(baseUrl);
		return trimmed == null ? null : trimmed.replaceAll("/+$", "");
	}

	private String normalizeHealthPath(String healthPath) {
		String trimmed = trimToNull(healthPath);
		return trimmed == null ? "/api/tags" : trimmed;
	}

	private String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String encrypt(String value) {
		try {
			return cipher.encrypt(value);
		} catch (ApiException exception) {
			throw new ApiException(ErrorCode.AI_BACKEND_ENCRYPT_FAILED);
		}
	}
}
