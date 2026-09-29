package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectTechStack;
import com.weai.server.domain.project.repository.ProjectTechStackRepository;
import com.weai.server.domain.project.response.BuildTaskExecutionResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectBuildExecutionService {

	private static final Set<String> ALLOWED_TASKS = Set.of(
		"bootrun", "build", "test", "clean", "bootjar", "check",
		"dependencies", "compilejava", "compiletestjava", "processresources", "classes",
		"spring-boot:run", "package", "verify", "dependency:tree"
	);

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final ProjectService projectService;
	private final UserService userService;
	private final ProjectTechStackRepository projectTechStackRepository;
	private final ProjectWorkspaceService projectWorkspaceService;

	public BuildTaskExecutionResponse executeTask(String userEmail, Long projectId, String rawTaskName) {
		Project project = getLeaderAccessibleProject(userEmail, projectId);
		String buildTool = resolveBuildTool(project.getId());
		File workingDir = resolveWorkingDirectory(project.getId());

		return runProcess(buildTool, rawTaskName, workingDir);
	}

	public BuildTaskExecutionResponse executeSystemTask(String rawTaskName) {
		File workingDir = new File(System.getProperty("user.dir"));
		String buildTool = detectBuildToolInDir(workingDir);

		return runProcess(buildTool, rawTaskName, workingDir);
	}

	private BuildTaskExecutionResponse runProcess(String buildTool, String rawTaskName, File workingDir) {
		String taskName = validateAndNormalizeTask(rawTaskName);
		boolean isWindows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

		List<String> commandList = buildCommand(buildTool, taskName, isWindows, workingDir);
		String commandDisplay = String.join(" ", commandList);

		log.info("Executing build task: [{}] in directory: [{}]", commandDisplay, workingDir.getAbsolutePath());

		long startTime = System.currentTimeMillis();
		List<String> outputLogs = new ArrayList<>();
		int exitCode = -1;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder(commandList);
			processBuilder.directory(workingDir);
			processBuilder.redirectErrorStream(true);

			Process process = processBuilder.start();

			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
			)) {
				String line;
				while ((line = reader.readLine()) != null) {
					outputLogs.add(line);
				}
			}

			boolean finished = process.waitFor(180, TimeUnit.SECONDS);
			if (finished) {
				exitCode = process.exitValue();
			} else {
				process.destroyForcibly();
				outputLogs.add("[ERROR] Build task timed out after 180 seconds.");
				exitCode = 124;
			}
		} catch (Exception e) {
			log.error("Failed to execute build task process", e);
			outputLogs.add("[ERROR] Failed to execute build process: " + e.getMessage());
			exitCode = 1;
		}

		long elapsedMs = System.currentTimeMillis() - startTime;
		String duration = String.format(Locale.US, "%.2fs", elapsedMs / 1000.0);
		String status = exitCode == 0 ? "SUCCESS" : "FAILED";
		String executedAt = LocalTime.now().format(TIME_FORMATTER);

		return new BuildTaskExecutionResponse(
			rawTaskName,
			commandDisplay,
			status,
			exitCode,
			duration,
			outputLogs,
			executedAt
		);
	}

	private List<String> buildCommand(String buildTool, String taskName, boolean isWindows, File workingDir) {
		List<String> cmd = new ArrayList<>();
		if ("MAVEN".equalsIgnoreCase(buildTool)) {
			String executable = resolveWrapperExecutable(workingDir, isWindows ? "mvnw.cmd" : "./mvnw", "mvn", isWindows);
			if (isWindows) {
				cmd.add("cmd.exe");
				cmd.add("/c");
			}
			cmd.add(executable);
			cmd.add(taskName);
		} else {
			String executable = resolveWrapperExecutable(workingDir, isWindows ? "gradlew.bat" : "./gradlew", "gradle", isWindows);
			if (isWindows) {
				cmd.add("cmd.exe");
				cmd.add("/c");
			}
			cmd.add(executable);
			cmd.add(taskName);
		}
		return cmd;
	}

	/**
	 * On Windows, {@code cmd.exe /c <bare-filename>.bat} does not reliably search the current
	 * working directory the way an interactive cmd.exe session does (this environment's `cmd.exe
	 * /c gradlew.bat` failed with "not recognized" even though `dir` in the same ProcessBuilder
	 * working directory showed the file) - it needs an explicit `.\` prefix to resolve a wrapper
	 * script sitting in the project's workspace directory. The bare global fallback (`gradle`/`mvn`)
	 * is left unprefixed since that one is meant to resolve via PATH, not the working directory.
	 */
	private String resolveWrapperExecutable(File workingDir, String wrapperRelativePath, String globalFallback, boolean isWindows) {
		File wrapperFile = new File(workingDir, wrapperRelativePath);
		if (!wrapperFile.exists()) {
			return isWindows ? globalFallback : wrapperRelativePath;
		}
		return isWindows ? ".\\" + wrapperRelativePath : wrapperRelativePath;
	}

	private String validateAndNormalizeTask(String rawTaskName) {
		if (rawTaskName == null || rawTaskName.isBlank()) {
			throw new ApiException(ErrorCode.INVALID_INPUT);
		}
		String normalized = rawTaskName.trim();
		String lower = normalized.toLowerCase(Locale.ROOT);
		if (!ALLOWED_TASKS.contains(lower) && !ALLOWED_TASKS.contains(lower.replace(":", ""))) {
			throw new ApiException(ErrorCode.INVALID_INPUT);
		}
		return normalized;
	}

	private Project getLeaderAccessibleProject(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		return projectService.validateProjectLeaderAccess(projectId, user.getId());
	}

	private String resolveBuildTool(Long projectId) {
		List<ProjectTechStack> techStacks = projectTechStackRepository.findByProject_IdOrderByCategoryAscIdAsc(projectId);
		boolean hasMaven = techStacks.stream()
			.map(ProjectTechStack::getName)
			.anyMatch(name -> name != null && name.toLowerCase(Locale.ROOT).contains("maven"));
		return hasMaven ? "MAVEN" : "GRADLE";
	}

	private String detectBuildToolInDir(File dir) {
		if (new File(dir, "pom.xml").exists() || new File(dir, "mvnw").exists() || new File(dir, "mvnw.cmd").exists()) {
			return "MAVEN";
		}
		return "GRADLE";
	}

	// 예전엔 project.getLocalPath()(클라이언트 PC 경로)를 서버에서 그대로 파일 경로로 취급했다 -
	// 원격 중앙 서버 배포에서는 그 경로가 서버 디스크에 존재할 리 없다. 이제 빌드는 프로젝트가
	// 업로드한 서버 측 워크스페이스 스냅샷(ProjectWorkspaceService)을 대상으로 실행한다.
	private File resolveWorkingDirectory(Long projectId) {
		return projectWorkspaceService.requireProjectDirectory(projectId).toFile();
	}
}
