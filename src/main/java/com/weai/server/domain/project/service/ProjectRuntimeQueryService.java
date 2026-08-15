package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectTechStack;
import com.weai.server.domain.project.repository.ProjectTechStackRepository;
import com.weai.server.domain.project.response.BuildTaskListResponse;
import com.weai.server.domain.project.response.BuildTaskListResponse.BuildTaskResponse;
import com.weai.server.domain.project.response.ProfileRunCommandListResponse;
import com.weai.server.domain.project.response.ProfileRunCommandListResponse.ProfileRunCommandResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectRuntimeQueryService {

	private static final Set<String> SUPPORTED_PROFILES = Set.of("local", "dev", "test", "prod");

	private final ProjectService projectService;
	private final UserService userService;
	private final ProjectTechStackRepository projectTechStackRepository;

	public BuildTaskListResponse getBuildTasks(String userEmail, Long projectId) {
		Project project = getAccessibleProject(userEmail, projectId);
		String buildTool = resolveBuildTool(project.getId());

		return new BuildTaskListResponse(
			project.getId(),
			buildTool,
			buildTasks(buildTool)
		);
	}

	public ProfileRunCommandListResponse getProfileRunCommands(
		String userEmail,
		Long projectId,
		String rawProfile
	) {
		Project project = getAccessibleProject(userEmail, projectId);
		String buildTool = resolveBuildTool(project.getId());
		String profile = normalizeProfile(rawProfile);
		List<String> profiles = profile == null ? List.of("local", "dev", "test", "prod") : List.of(profile);

		return new ProfileRunCommandListResponse(
			project.getId(),
			buildTool,
			resolveOsType(),
			profiles.stream()
				.map(candidate -> buildRunCommand(buildTool, candidate, profile == null || candidate.equals(profile)))
				.toList()
		);
	}

	private Project getAccessibleProject(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		return projectService.validateProjectAccess(projectId, user.getId());
	}

	private String resolveBuildTool(Long projectId) {
		List<ProjectTechStack> techStacks = projectTechStackRepository.findByProject_IdOrderByCategoryAscIdAsc(projectId);
		boolean hasMaven = techStacks.stream()
			.map(ProjectTechStack::getName)
			.anyMatch(name -> containsIgnoreCase(name, "maven"));
		if (hasMaven) {
			return "MAVEN";
		}
		return "GRADLE";
	}

	private List<BuildTaskResponse> buildTasks(String buildTool) {
		if ("MAVEN".equals(buildTool)) {
			return List.of(
				new BuildTaskResponse("spring-boot:run", "Spring Boot 실행", "Spring Boot 애플리케이션을 실행합니다.", "./mvnw spring-boot:run", "RUN", false, true),
				new BuildTaskResponse("package", "패키징", "애플리케이션 패키지를 생성합니다.", "./mvnw package", "BUILD", false, true),
				new BuildTaskResponse("test", "테스트", "프로젝트 테스트를 실행합니다.", "./mvnw test", "TEST", false, true),
				new BuildTaskResponse("clean", "Clean", "빌드 결과물을 삭제합니다.", "./mvnw clean", "BUILD", true, true),
				new BuildTaskResponse("dependency:tree", "의존성 트리", "프로젝트 의존성 트리를 조회합니다.", "./mvnw dependency:tree", "INFO", false, true),
				new BuildTaskResponse("verify", "검증", "테스트와 검증 단계를 실행합니다.", "./mvnw verify", "TEST", false, true)
			);
		}

		return List.of(
			new BuildTaskResponse("bootRun", "Spring Boot 실행", "Spring Boot 애플리케이션을 실행합니다.", "./gradlew bootRun", "RUN", false, true),
			new BuildTaskResponse("build", "Build", "프로젝트를 빌드합니다.", "./gradlew build", "BUILD", false, true),
			new BuildTaskResponse("test", "Test", "프로젝트 테스트를 실행합니다.", "./gradlew test", "TEST", false, true),
			new BuildTaskResponse("clean", "Clean", "빌드 결과물을 삭제합니다.", "./gradlew clean", "BUILD", true, true),
			new BuildTaskResponse("dependencies", "Dependencies", "프로젝트 의존성 정보를 조회합니다.", "./gradlew dependencies", "INFO", false, true),
			new BuildTaskResponse("bootJar", "Boot Jar", "실행 가능한 Spring Boot jar를 생성합니다.", "./gradlew bootJar", "BUILD", false, true),
			new BuildTaskResponse("check", "Check", "검증 태스크를 실행합니다.", "./gradlew check", "TEST", false, true)
		);
	}

	private ProfileRunCommandResponse buildRunCommand(String buildTool, String profile, boolean active) {
		String description = switch (profile) {
			case "local" -> "로컬 환경 실행 명령입니다.";
			case "dev" -> "개발 환경 실행 명령입니다.";
			case "test" -> "테스트 환경 실행 명령입니다.";
			case "prod" -> "운영 환경 실행 명령입니다.";
			default -> "프로파일 실행 명령입니다.";
		};

		String windowsCommand = "MAVEN".equals(buildTool)
			? "$env:SPRING_PROFILES_ACTIVE=\"%s\"; ./mvnw.cmd spring-boot:run".formatted(profile)
			: "$env:SPRING_PROFILES_ACTIVE=\"%s\"; ./gradlew.bat bootRun".formatted(profile);
		String unixCommand = "MAVEN".equals(buildTool)
			? "SPRING_PROFILES_ACTIVE=%s ./mvnw spring-boot:run".formatted(profile)
			: "SPRING_PROFILES_ACTIVE=%s ./gradlew bootRun".formatted(profile);

		return new ProfileRunCommandResponse(profile, description, windowsCommand, unixCommand, active);
	}

	private String normalizeProfile(String rawProfile) {
		if (rawProfile == null || rawProfile.isBlank()) {
			return null;
		}
		String profile = rawProfile.trim().toLowerCase(Locale.ROOT);
		if (!SUPPORTED_PROFILES.contains(profile)) {
			throw new ApiException(ErrorCode.INVALID_SPRING_PROFILE);
		}
		return profile;
	}

	private String resolveOsType() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (osName.contains("win")) {
			return "WINDOWS";
		}
		if (osName.contains("mac")) {
			return "MAC";
		}
		return "LINUX";
	}

	private boolean containsIgnoreCase(String value, String needle) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
	}
}
