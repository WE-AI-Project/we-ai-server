package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectEnvironmentVariable;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.repository.ProjectEnvironmentVariableRepository;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.request.ProjectEnvironmentVariableCreateRequest;
import com.weai.server.domain.project.request.ProjectEnvironmentVariableUpdateRequest;
import com.weai.server.domain.project.response.ProjectEnvironmentVariableDeleteResponse;
import com.weai.server.domain.project.response.ProjectEnvironmentVariableListResponse;
import com.weai.server.domain.project.response.ProjectEnvironmentVariableListResponse.EnvironmentVariableResponse;
import com.weai.server.domain.project.response.ProjectEnvironmentVariableMutationResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectEnvironmentVariableService {

	private static final Set<String> SUPPORTED_PROFILES = Set.of("local", "dev", "test", "prod");
	private static final Pattern VARIABLE_KEY_PATTERN = Pattern.compile("^[A-Z_][A-Z0-9_]*$");
	private static final String MASKED_VALUE = "********";

	private final ProjectService projectService;
	private final UserService userService;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectEnvironmentVariableRepository projectEnvironmentVariableRepository;
	private final ProjectEnvironmentValueCipher environmentValueCipher;

	public ProjectEnvironmentVariableListResponse getEnvironmentVariablesForView(
		String userEmail,
		Long projectId,
		String rawProfile,
		Boolean enabled
	) {
		getAccessibleProject(userEmail, projectId);
		String profile = rawProfile == null || rawProfile.isBlank() ? null : validateProfile(rawProfile);
		List<EnvironmentVariableResponse> variables = projectEnvironmentVariableRepository
			.findActiveVariables(projectId, profile, enabled)
			.stream()
			.map(this::toViewResponse)
			.toList();

		return new ProjectEnvironmentVariableListResponse(projectId, profile, variables.size(), variables);
	}

	@Transactional
	public ProjectEnvironmentVariableMutationResponse createEnvironmentVariable(
		String userEmail,
		Long projectId,
		ProjectEnvironmentVariableCreateRequest request
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		validateLeader(projectId, user.getId());
		String variableKey = validateVariableKey(request == null ? null : request.key());
		String value = validateRequiredValue(request == null ? null : request.value());
		String profile = validateRequiredProfile(request == null ? null : request.profile());
		validateDuplicate(projectId, profile, variableKey, null);

		ProjectEnvironmentVariable variable = ProjectEnvironmentVariable.create(
			project,
			profile,
			variableKey,
			environmentValueCipher.encrypt(value),
			request.secret() != null && request.secret(),
			trimToNull(request.description()),
			request.enabled() == null || request.enabled(),
			user
		);
		return toMutationResponse(projectEnvironmentVariableRepository.save(variable));
	}

	@Transactional
	public ProjectEnvironmentVariableMutationResponse updateEnvironmentVariable(
		String userEmail,
		Long projectId,
		Long environmentVariableId,
		ProjectEnvironmentVariableUpdateRequest request
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		validateLeader(projectId, user.getId());
		ProjectEnvironmentVariable variable = getVariable(projectId, environmentVariableId);

		String profile = request == null || request.profile() == null
			? variable.getProfile()
			: validateRequiredProfile(request.profile());
		validateDuplicate(projectId, profile, variable.getVariableKey(), environmentVariableId);
		String encryptedValue = request == null || request.value() == null
			? variable.getEncryptedValue()
			: environmentValueCipher.encrypt(validateRequiredValue(request.value()));
		boolean secret = request == null || request.secret() == null ? variable.isSecret() : request.secret();
		String description = request == null || request.description() == null
			? variable.getDescription()
			: trimToNull(request.description());
		boolean enabled = request == null || request.enabled() == null ? variable.isEnabled() : request.enabled();

		variable.update(profile, encryptedValue, secret, description, enabled, user);
		return toMutationResponse(variable);
	}

	@Transactional
	public ProjectEnvironmentVariableDeleteResponse deleteEnvironmentVariable(
		String userEmail,
		Long projectId,
		Long environmentVariableId
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		validateLeader(projectId, user.getId());
		ProjectEnvironmentVariable variable = getVariable(projectId, environmentVariableId);
		variable.delete(user);
		return new ProjectEnvironmentVariableDeleteResponse(variable.getEnvironmentVariableId(), variable.getDeletedAt());
	}

	/**
	 * Returns decrypted values only for process execution. Never use this data in controller responses or logs.
	 */
	public Map<String, String> getEnvironmentVariables(Long projectId, String rawProfile) {
		String profile = validateRequiredProfile(rawProfile);
		Map<String, String> variables = new LinkedHashMap<>();
		projectEnvironmentVariableRepository
			.findByProject_IdAndProfileAndEnabledTrueAndDeletedAtIsNullOrderByVariableKeyAsc(projectId, profile)
			.forEach(variable -> variables.put(variable.getVariableKey(), environmentValueCipher.decrypt(variable.getEncryptedValue())));
		return variables;
	}

	private Project getAccessibleProject(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		return projectService.validateProjectAccess(projectId, user.getId());
	}

	private ProjectEnvironmentVariable getVariable(Long projectId, Long environmentVariableId) {
		return projectEnvironmentVariableRepository
			.findByEnvironmentVariableIdAndProject_IdAndDeletedAtIsNull(environmentVariableId, projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.ENVIRONMENT_VARIABLE_NOT_FOUND));
	}

	private void validateLeader(Long projectId, Long userId) {
		ProjectMember member = projectMemberRepository
			.findByProject_IdAndUser_IdAndStatus(projectId, userId, ProjectMemberStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_ACCESS_DENIED));
		if (!member.isLeader()) {
			throw new ApiException(ErrorCode.PROJECT_LEADER_ONLY);
		}
	}

	private void validateDuplicate(Long projectId, String profile, String variableKey, Long currentVariableId) {
		projectEnvironmentVariableRepository
			.findByProject_IdAndProfileAndVariableKeyAndDeletedAtIsNull(projectId, profile, variableKey)
			.filter(variable -> !variable.getEnvironmentVariableId().equals(currentVariableId))
			.ifPresent(variable -> {
				throw new ApiException(ErrorCode.ENVIRONMENT_VARIABLE_ALREADY_EXISTS);
			});
	}

	private EnvironmentVariableResponse toViewResponse(ProjectEnvironmentVariable variable) {
		boolean secret = variable.isSecret();
		return new EnvironmentVariableResponse(
			variable.getEnvironmentVariableId(),
			variable.getVariableKey(),
			secret ? null : environmentValueCipher.decrypt(variable.getEncryptedValue()),
			secret ? MASKED_VALUE : null,
			variable.getProfile(),
			secret,
			variable.getDescription(),
			variable.isEnabled(),
			variable.getCreatedAt(),
			variable.getUpdatedAt()
		);
	}

	private ProjectEnvironmentVariableMutationResponse toMutationResponse(ProjectEnvironmentVariable variable) {
		return new ProjectEnvironmentVariableMutationResponse(
			variable.getEnvironmentVariableId(),
			variable.getVariableKey(),
			variable.getProfile(),
			variable.isSecret(),
			variable.isSecret() ? MASKED_VALUE : null,
			variable.getDescription(),
			variable.isEnabled(),
			variable.getCreatedAt(),
			variable.getUpdatedAt()
		);
	}

	private String validateVariableKey(String rawVariableKey) {
		String variableKey = trimToNull(rawVariableKey);
		if (variableKey == null) {
			throw new ApiException(ErrorCode.ENVIRONMENT_VARIABLE_KEY_REQUIRED);
		}
		if (variableKey.length() > 100 || !VARIABLE_KEY_PATTERN.matcher(variableKey).matches()) {
			throw new ApiException(ErrorCode.INVALID_ENVIRONMENT_VARIABLE_KEY);
		}
		return variableKey;
	}

	private String validateRequiredValue(String value) {
		if (value == null || value.isBlank()) {
			throw new ApiException(ErrorCode.ENVIRONMENT_VARIABLE_VALUE_REQUIRED);
		}
		return value;
	}

	private String validateRequiredProfile(String rawProfile) {
		if (rawProfile == null || rawProfile.isBlank()) {
			throw new ApiException(ErrorCode.SPRING_PROFILE_REQUIRED);
		}
		return validateProfile(rawProfile);
	}

	private String validateProfile(String rawProfile) {
		String profile = rawProfile.trim().toLowerCase(Locale.ROOT);
		if (!SUPPORTED_PROFILES.contains(profile)) {
			throw new ApiException(ErrorCode.INVALID_SPRING_PROFILE);
		}
		return profile;
	}

	private String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
