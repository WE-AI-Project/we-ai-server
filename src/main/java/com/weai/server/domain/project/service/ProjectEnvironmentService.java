package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.BuildTool;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectEnvironmentSetting;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.repository.ProjectEnvironmentSettingRepository;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.request.ActiveSpringProfileUpdateRequest;
import com.weai.server.domain.project.response.ActiveSpringProfileUpdateResponse;
import com.weai.server.domain.project.response.RuntimeEnvironmentResponse;
import com.weai.server.domain.project.response.RuntimeEnvironmentResponse.MemoryResponse;
import com.weai.server.domain.project.response.SpringProfileListResponse;
import com.weai.server.domain.project.response.SpringProfileListResponse.SpringProfileResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectEnvironmentService {

	private static final String DEFAULT_ACTIVE_PROFILE = "dev";
	private static final Set<String> SUPPORTED_PROFILES = Set.of("local", "dev", "test", "prod");
	private static final List<ProfileDefinition> DEFAULT_PROFILES = List.of(
		new ProfileDefinition("local", "Local", "로컬 개발 환경"),
		new ProfileDefinition("dev", "Development", "개발 서버 환경"),
		new ProfileDefinition("test", "Test", "테스트 환경"),
		new ProfileDefinition("prod", "Production", "운영 환경")
	);

	private final ProjectService projectService;
	private final UserService userService;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectEnvironmentSettingRepository projectEnvironmentSettingRepository;
	private final Environment environment;

	public RuntimeEnvironmentResponse getRuntimeEnvironment(String userEmail, Long projectId) {
		Project project = getAccessibleProject(userEmail, projectId);
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		return new RuntimeEnvironmentResponse(
			project.getId(),
			project.getProjectName(),
			System.getProperty("java.version"),
			System.getProperty("java.vm.name"),
			System.getProperty("java.vendor"),
			System.getProperty("os.name"),
			System.getProperty("os.version"),
			System.getProperty("os.arch"),
			runtime.availableProcessors(),
			ZoneId.systemDefault().getId(),
			LocalDateTime.now(),
			SpringBootVersion.getVersion(),
			Arrays.asList(environment.getActiveProfiles()),
			Arrays.asList(environment.getDefaultProfiles()),
			new MemoryResponse(
				runtime.maxMemory(),
				totalMemory,
				freeMemory,
				totalMemory - freeMemory
			)
		);
	}

	public SpringProfileListResponse getSpringProfiles(String userEmail, Long projectId) {
		Project project = getAccessibleProject(userEmail, projectId);
		String activeProfile = resolveActiveProfile(project.getId());

		return new SpringProfileListResponse(
			project.getId(),
			activeProfile,
			DEFAULT_PROFILES.stream()
				.map(profile -> new SpringProfileResponse(
					profile.name(),
					profile.displayName(),
					profile.description(),
					profile.name().equals(activeProfile),
					false
				))
				.toList()
		);
	}

	@Transactional
	public ActiveSpringProfileUpdateResponse updateActiveProfile(
		String userEmail,
		Long projectId,
		ActiveSpringProfileUpdateRequest request
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		validateLeader(projectId, user.getId());
		String activeProfile = validateRequiredProfile(request == null ? null : request.profile());
		Optional<ProjectEnvironmentSetting> existingSetting = projectEnvironmentSettingRepository.findByProject_Id(projectId);
		String previousProfile = existingSetting
			.map(ProjectEnvironmentSetting::getActiveProfile)
			.orElseGet(() -> resolveEnvironmentProfile().orElse(DEFAULT_ACTIVE_PROFILE));

		ProjectEnvironmentSetting setting = existingSetting
			.orElseGet(() -> ProjectEnvironmentSetting.create(
				project,
				previousProfile,
				BuildTool.GRADLE,
				System.getProperty("java.version"),
				user
			));
		setting.updateActiveProfile(activeProfile, user);
		ProjectEnvironmentSetting savedSetting = projectEnvironmentSettingRepository.save(setting);

		return new ActiveSpringProfileUpdateResponse(
			project.getId(),
			previousProfile,
			savedSetting.getActiveProfile(),
			user.getId(),
			savedSetting.getChangedAt(),
			false,
			true
		);
	}

	public Optional<String> findStoredActiveProfile(Long projectId) {
		return projectEnvironmentSettingRepository.findByProject_Id(projectId)
			.map(ProjectEnvironmentSetting::getActiveProfile)
			.filter(SUPPORTED_PROFILES::contains);
	}

	private Project getAccessibleProject(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		return projectService.validateProjectAccess(projectId, user.getId());
	}

	private void validateLeader(Long projectId, Long userId) {
		ProjectMember member = projectMemberRepository
			.findByProject_IdAndUser_IdAndStatus(projectId, userId, ProjectMemberStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_ACCESS_DENIED));
		if (!member.isLeader()) {
			throw new ApiException(ErrorCode.PROJECT_LEADER_ONLY);
		}
	}

	private String resolveActiveProfile(Long projectId) {
		return findStoredActiveProfile(projectId)
			.or(() -> resolveEnvironmentProfile())
			.orElse(DEFAULT_ACTIVE_PROFILE);
	}

	private Optional<String> resolveEnvironmentProfile() {
		return Arrays.stream(environment.getActiveProfiles())
			.map(profile -> profile == null ? null : profile.trim().toLowerCase(Locale.ROOT))
			.filter(SUPPORTED_PROFILES::contains)
			.findFirst();
	}

	private String validateRequiredProfile(String rawProfile) {
		if (rawProfile == null || rawProfile.isBlank()) {
			throw new ApiException(ErrorCode.SPRING_PROFILE_REQUIRED);
		}
		String profile = rawProfile.trim().toLowerCase(Locale.ROOT);
		if (!SUPPORTED_PROFILES.contains(profile)) {
			throw new ApiException(ErrorCode.INVALID_SPRING_PROFILE);
		}
		return profile;
	}

	private record ProfileDefinition(String name, String displayName, String description) {
	}
}
