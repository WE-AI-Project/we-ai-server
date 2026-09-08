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

	public BuildTaskExecutionResponse executeTask(String userEmail, Long projectId, String rawTaskName) {
		Project project = getAccessibleProject(userEmail, projectId);
		String buildTool = resolveBuildTool(project.getId());
		File workingDir = resolveWorkingDirectory(project.getLocalPath());

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
			String executable = isWindows ? "mvnw.cmd" : "./mvnw";
			File execFile = new File(workingDir, executable);
			if (!execFile.exists() && isWindows) {
				executable = "mvn";
			}
			if (isWindows) {
				cmd.add("cmd.exe");
				cmd.add("/c");
			}
			cmd.add(executable);
			cmd.add(taskName);
		} else {
			String executable = isWindows ? "gradlew.bat" : "./gradlew";
			File execFile = new File(workingDir, executable);
			if (!execFile.exists() && isWindows) {
				executable = "gradle";
			}
			if (isWindows) {
				cmd.add("cmd.exe");
				cmd.add("/c");
			}
			cmd.add(executable);
			cmd.add(taskName);
		}
		return cmd;
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

	private Project getAccessibleProject(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		return projectService.validateProjectAccess(projectId, user.getId());
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

	private File resolveWorkingDirectory(String localPath) {
		if (localPath != null && !localPath.isBlank()) {
			File candidate = new File(localPath.trim());
			if (candidate.exists() && candidate.isDirectory()) {
				return candidate;
			}
		}
		return new File(System.getProperty("user.dir"));
	}
}
