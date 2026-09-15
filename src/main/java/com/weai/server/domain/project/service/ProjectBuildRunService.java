package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.BuildRun;
import com.weai.server.domain.project.domain.BuildRunStatus;
import com.weai.server.domain.project.domain.BuildTool;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.repository.BuildRunRepository;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.request.BuildTaskRunRequest;
import com.weai.server.domain.project.response.BuildRunHistoryResponse;
import com.weai.server.domain.project.response.BuildRunHistoryResponse.BuildRunSummaryResponse;
import com.weai.server.domain.project.response.BuildRunResultResponse;
import com.weai.server.domain.project.response.BuildTaskRunResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import jakarta.persistence.criteria.Predicate;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectBuildRunService {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final Set<BuildRunStatus> ACTIVE_BUILD_STATUSES = Set.of(BuildRunStatus.QUEUED, BuildRunStatus.RUNNING);
	private static final Map<String, String> ALLOWED_TASKS = Map.of(
		"build", "build",
		"test", "test",
		"clean", "clean",
		"bootjar", "bootJar",
		"check", "check"
	);
	private static final Set<String> ALLOWED_PROFILES = Set.of("local", "dev", "test", "prod", "stag");

	private final ProjectService projectService;
	private final UserService userService;
	private final ProjectMemberRepository projectMemberRepository;
	private final BuildRunRepository buildRunRepository;
	private final BuildRunExecutionWorker buildRunExecutionWorker;
	private final ProjectEnvironmentService projectEnvironmentService;
	@Qualifier("buildTaskExecutor")
	private final TaskExecutor buildTaskExecutor;

	@Transactional
	public BuildTaskRunResponse runBuildTask(String userEmail, Long projectId, BuildTaskRunRequest request) {
		User requester = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, requester.getId());
		validateLeader(projectId, requester.getId());

		String taskName = validateTaskName(request == null ? null : request.taskName());
		String profile = resolveProfile(projectId, request == null ? null : request.profile());
		Path projectPath = validateProjectLocalPath(project);
		Path gradleWrapper = resolveGradleWrapper(projectPath);
		if (buildRunRepository.existsByProject_IdAndStatusIn(projectId, ACTIVE_BUILD_STATUSES)) {
			throw new ApiException(ErrorCode.BUILD_ALREADY_RUNNING);
		}

		BuildCommand command = buildCommand(projectPath, gradleWrapper, taskName, profile);
		BuildRun buildRun = buildRunRepository.saveAndFlush(BuildRun.start(
			project,
			requester,
			taskName,
			BuildTool.GRADLE,
			profile,
			command.display()
		));

		submitAfterCommit(buildRun.getBuildRunId(), command);
		return toRunResponse(buildRun);
	}

	public BuildRunResultResponse getBuildRunResult(String userEmail, Long projectId, Long buildRunId) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		BuildRun buildRun = getBuildRun(projectId, buildRunId);
		return toResultResponse(buildRun);
	}

	public BuildRunHistoryResponse getBuildRunHistory(
		String userEmail,
		Long projectId,
		Integer rawPage,
		Integer rawSize,
		String rawStatus,
		String rawTaskName,
		String rawProfile
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		BuildRunStatus status = parseStatus(rawStatus);
		String taskName = rawTaskName == null ? null : validateTaskName(rawTaskName);
		String profile = validateProfile(rawProfile);
		Pageable pageable = pageRequest(rawPage, rawSize);
		Page<BuildRun> runs = buildRunRepository.findAll(historySpecification(projectId, status, taskName, profile), pageable);

		return new BuildRunHistoryResponse(
			projectId,
			runs.getNumber(),
			runs.getSize(),
			runs.getTotalPages(),
			buildRunRepository.count((root, query, criteriaBuilder) ->
				criteriaBuilder.equal(root.get("project").get("id"), projectId)),
			buildRunRepository.count(historySpecification(projectId, BuildRunStatus.SUCCESS, null, null)),
			buildRunRepository.count(historySpecification(projectId, BuildRunStatus.FAILED, null, null)),
			buildRunRepository.count((root, query, criteriaBuilder) ->
				criteriaBuilder.and(
					criteriaBuilder.equal(root.get("project").get("id"), projectId),
					root.get("status").in(ACTIVE_BUILD_STATUSES)
				)),
			runs.getContent().stream().map(this::toSummaryResponse).toList()
		);
	}

	private void validateLeader(Long projectId, Long userId) {
		ProjectMember member = projectMemberRepository
			.findByProject_IdAndUser_IdAndStatus(projectId, userId, ProjectMemberStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_ACCESS_DENIED));
		if (!member.isLeader()) {
			throw new ApiException(ErrorCode.PROJECT_LEADER_ONLY);
		}
	}

	private Path validateProjectLocalPath(Project project) {
		if (project.getLocalPath() == null || project.getLocalPath().isBlank()) {
			throw new ApiException(ErrorCode.PROJECT_LOCAL_PATH_NOT_FOUND);
		}

		Path projectPath = Path.of(project.getLocalPath().trim()).toAbsolutePath().normalize();
		if (!Files.isDirectory(projectPath)) {
			throw new ApiException(ErrorCode.PROJECT_LOCAL_PATH_NOT_FOUND);
		}
		return projectPath;
	}

	private Path resolveGradleWrapper(Path projectPath) {
		boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
		Path wrapper = projectPath.resolve(windows ? "gradlew.bat" : "gradlew").normalize();
		if (!Files.isRegularFile(wrapper)) {
			throw new ApiException(ErrorCode.GRADLE_WRAPPER_NOT_FOUND);
		}
		return wrapper;
	}

	private BuildCommand buildCommand(Path projectPath, Path gradleWrapper, String taskName, String profile) {
		List<String> arguments = new ArrayList<>();
		arguments.add(gradleWrapper.toString());
		arguments.add(taskName);
		if (profile != null) {
			arguments.add("-Dspring.profiles.active=" + profile);
		}

		List<String> displayArguments = new ArrayList<>();
		displayArguments.add(gradleWrapper.getFileName().toString());
		displayArguments.add(taskName);
		if (profile != null) {
			displayArguments.add("-Dspring.profiles.active=" + profile);
		}
		return new BuildCommand(projectPath.toFile(), arguments, String.join(" ", displayArguments));
	}

	private String validateTaskName(String rawTaskName) {
		String taskName = trimToNull(rawTaskName);
		if (taskName == null) {
			throw new ApiException(ErrorCode.BUILD_TASK_NAME_REQUIRED);
		}
		String normalized = ALLOWED_TASKS.get(taskName.toLowerCase(Locale.ROOT));
		if (normalized == null) {
			throw new ApiException(ErrorCode.INVALID_BUILD_TASK_NAME);
		}
		return normalized;
	}

	private String validateProfile(String rawProfile) {
		String profile = trimToNull(rawProfile);
		if (profile == null) {
			return null;
		}
		String normalized = profile.toLowerCase(Locale.ROOT);
		if (!ALLOWED_PROFILES.contains(normalized)) {
			throw new ApiException(ErrorCode.INVALID_SPRING_PROFILE);
		}
		return normalized;
	}

	private String resolveProfile(Long projectId, String rawProfile) {
		String requestedProfile = validateProfile(rawProfile);
		if (requestedProfile != null) {
			return requestedProfile;
		}
		return projectEnvironmentService.findStoredActiveProfile(projectId).orElse(null);
	}

	private BuildRunStatus parseStatus(String rawStatus) {
		String status = trimToNull(rawStatus);
		if (status == null) {
			return null;
		}
		try {
			return BuildRunStatus.valueOf(status.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_BUILD_RUN_STATUS);
		}
	}

	private Pageable pageRequest(Integer rawPage, Integer rawSize) {
		int page = rawPage == null ? 0 : rawPage;
		int size = rawSize == null ? DEFAULT_PAGE_SIZE : rawSize;
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "page must be non-negative and size must be between 1 and 100.");
		}
		return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	private Specification<BuildRun> historySpecification(
		Long projectId,
		BuildRunStatus status,
		String taskName,
		String profile
	) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("project").get("id"), projectId));
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (taskName != null) {
				predicates.add(criteriaBuilder.equal(root.get("taskName"), taskName));
			}
			if (profile != null) {
				predicates.add(criteriaBuilder.equal(root.get("profile"), profile));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private BuildRun getBuildRun(Long projectId, Long buildRunId) {
		return buildRunRepository.findByBuildRunIdAndProject_Id(buildRunId, projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.BUILD_RUN_NOT_FOUND));
	}

	private void submitAfterCommit(Long buildRunId, BuildCommand command) {
		Runnable task = () -> {
			try {
				buildRunExecutionWorker.execute(buildRunId, command);
			} catch (RuntimeException exception) {
				log.error("Failed to execute build run asynchronously. buildRunId={}", buildRunId, exception);
			}
		};

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					submit(task);
				}
			});
			return;
		}

		submit(task);
	}

	private void submit(Runnable task) {
		try {
			buildTaskExecutor.execute(task);
		} catch (RuntimeException exception) {
			throw new ApiException(ErrorCode.BUILD_TASK_RUN_FAILED, "Failed to submit the build task.");
		}
	}

	private BuildTaskRunResponse toRunResponse(BuildRun buildRun) {
		return new BuildTaskRunResponse(
			buildRun.getBuildRunId(),
			buildRun.getProject().getId(),
			buildRun.getTaskName(),
			buildRun.getBuildTool(),
			buildRun.getProfile(),
			buildRun.getStatus(),
			buildRun.getCommand(),
			buildRun.getRequester().getId(),
			buildRun.getCreatedAt() == null ? buildRun.getStartedAt() : buildRun.getCreatedAt()
		);
	}

	private BuildRunResultResponse toResultResponse(BuildRun buildRun) {
		return new BuildRunResultResponse(
			buildRun.getBuildRunId(),
			buildRun.getProject().getId(),
			buildRun.getTaskName(),
			buildRun.getBuildTool(),
			buildRun.getProfile(),
			buildRun.getStatus(),
			buildRun.getCommand(),
			buildRun.getExitCode(),
			buildRun.getOutput(),
			buildRun.getErrorOutput(),
			buildRun.getStartedAt(),
			buildRun.getFinishedAt(),
			buildRun.getDurationMs(),
			buildRun.getRequester().getId(),
			buildRun.getRequester().getName()
		);
	}

	private BuildRunSummaryResponse toSummaryResponse(BuildRun buildRun) {
		return new BuildRunSummaryResponse(
			buildRun.getBuildRunId(),
			buildRun.getTaskName(),
			buildRun.getBuildTool(),
			buildRun.getProfile(),
			buildRun.getStatus(),
			buildRun.getExitCode(),
			buildRun.getDurationMs(),
			buildRun.getRequester().getId(),
			buildRun.getRequester().getName(),
			buildRun.getStartedAt(),
			buildRun.getFinishedAt(),
			buildRun.getCreatedAt()
		);
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
