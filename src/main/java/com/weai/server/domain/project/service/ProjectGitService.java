package com.weai.server.domain.project.service;

import com.weai.server.domain.project.config.ProjectGitProperties;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectRepositoryType;
import com.weai.server.domain.project.response.ProjectCommitDetailResponse;
import com.weai.server.domain.project.response.ProjectCommitFileDiffResponse;
import com.weai.server.domain.project.response.ProjectCommitFileListResponse;
import com.weai.server.domain.project.response.ProjectCommitListResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
public class ProjectGitService {

	private static final String COMMIT_MARKER = "__WEAI_COMMIT__";
	private static final String FIELD_SEPARATOR = "\u001f";
	private static final long GIT_COMMAND_TIMEOUT_SECONDS = 20L;

	private final ProjectService projectService;
	private final UserService userService;
	private final ProjectGitProperties projectGitProperties;

	public ProjectCommitListResponse getProjectCommits(
		String userEmail,
		Long projectId,
		String rawRepositoryType,
		Integer rawLimit
	) {
		ProjectRepositoryType repositoryType = ProjectRepositoryType.from(rawRepositoryType);
		int limit = normalizeLimit(rawLimit);
		Project project = getAccessibleProject(userEmail, projectId);
		Path repositoryPath = resolveRepositoryPath(project, repositoryType);
		List<GitCommitSummary> commits = readCommitSummaries(repositoryPath, limit);

		return new ProjectCommitListResponse(
			projectId,
			repositoryType,
			limit,
			commits.stream()
				.map(commit -> new ProjectCommitListResponse.CommitSummaryResponse(
					commit.commitHash(),
					commit.shortCommitHash(),
					commit.message(),
					commit.authorName(),
					commit.authorEmail(),
					commit.committedAt(),
					commit.changedFileCount(),
					commit.additions(),
					commit.deletions()
				))
				.toList()
		);
	}

	public ProjectCommitDetailResponse getProjectCommitDetail(
		String userEmail,
		Long projectId,
		String rawRepositoryType,
		String commitHash
	) {
		ProjectRepositoryType repositoryType = ProjectRepositoryType.from(rawRepositoryType);
		Project project = getAccessibleProject(userEmail, projectId);
		Path repositoryPath = resolveRepositoryPath(project, repositoryType);
		GitCommitMetadata metadata = readCommitMetadata(repositoryPath, commitHash);
		List<GitCommitFile> files = readCommitFiles(repositoryPath, commitHash);

		return new ProjectCommitDetailResponse(
			projectId,
			repositoryType,
			metadata.commitHash(),
			metadata.shortCommitHash(),
			metadata.message(),
			metadata.body(),
			metadata.authorName(),
			metadata.authorEmail(),
			metadata.committedAt(),
			files.size(),
			files.stream().mapToLong(GitCommitFile::additions).sum(),
			files.stream().mapToLong(GitCommitFile::deletions).sum()
		);
	}

	public ProjectCommitFileListResponse getProjectCommitFiles(
		String userEmail,
		Long projectId,
		String rawRepositoryType,
		String commitHash
	) {
		ProjectRepositoryType repositoryType = ProjectRepositoryType.from(rawRepositoryType);
		Project project = getAccessibleProject(userEmail, projectId);
		Path repositoryPath = resolveRepositoryPath(project, repositoryType);
		List<GitCommitFile> files = readCommitFiles(repositoryPath, commitHash);

		return new ProjectCommitFileListResponse(
			projectId,
			repositoryType,
			commitHash,
			files.stream()
				.map(file -> new ProjectCommitFileListResponse.CommitFileResponse(
					file.path(),
					file.fileName(),
					file.extension(),
					file.status(),
					file.additions(),
					file.deletions()
				))
				.toList()
		);
	}

	public ProjectCommitFileDiffResponse getProjectCommitFileDiff(
		String userEmail,
		Long projectId,
		String rawRepositoryType,
		String commitHash,
		String filePath
	) {
		if (filePath == null || filePath.isBlank()) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "filePath is required.");
		}

		ProjectRepositoryType repositoryType = ProjectRepositoryType.from(rawRepositoryType);
		Project project = getAccessibleProject(userEmail, projectId);
		Path repositoryPath = resolveRepositoryPath(project, repositoryType);
		GitCommitFile file = readCommitFiles(repositoryPath, commitHash).stream()
			.filter(candidate -> Objects.equals(candidate.path(), filePath))
			.findFirst()
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_COMMIT_FILE_NOT_FOUND));

		return new ProjectCommitFileDiffResponse(
			projectId,
			repositoryType,
			commitHash,
			file.path(),
			file.fileName(),
			file.extension(),
			file.status(),
			file.additions(),
			file.deletions(),
			file.diff()
		);
	}

	private Project getAccessibleProject(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		return projectService.validateProjectAccess(projectId, user.getId());
	}

	private int normalizeLimit(Integer rawLimit) {
		int fallbackLimit = projectGitProperties.getDefaultCommitLimit() > 0
			? projectGitProperties.getDefaultCommitLimit()
			: 20;
		int maxLimit = projectGitProperties.getMaxCommitLimit() > 0
			? projectGitProperties.getMaxCommitLimit()
			: 100;
		int limit = rawLimit == null ? fallbackLimit : rawLimit;
		if (limit <= 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "limit must be greater than 0.");
		}
		return Math.min(limit, maxLimit);
	}

	private Path resolveRepositoryPath(Project project, ProjectRepositoryType repositoryType) {
		return switch (repositoryType) {
			case BACKEND -> {
				List<String> candidates = new ArrayList<>();
				candidates.add(project.getLocalPath());
				candidates.add(projectGitProperties.getDefaultBackendLocalPath());
				candidates.add(System.getProperty("user.dir"));
				yield resolveGitDirectory(candidates);
			}
			case FRONTEND -> resolveFrontendRepositoryPath(project);
		};
	}

	private Path resolveFrontendRepositoryPath(Project project) {
		Set<String> candidates = new LinkedHashSet<>();
		candidates.add(projectGitProperties.getDefaultFrontendLocalPath());

		Path currentWorkingDirectory = safeToPath(System.getProperty("user.dir"));
		if (currentWorkingDirectory != null && currentWorkingDirectory.getParent() != null) {
			candidates.add(currentWorkingDirectory.getParent().resolve("we-ai-client").toString());
		}

		Path projectLocalPath = safeToPath(project.getLocalPath());
		if (projectLocalPath != null && projectLocalPath.getParent() != null) {
			candidates.add(projectLocalPath.getParent().resolve("we-ai-client").toString());
		}

		return resolveGitDirectory(candidates.stream().toList());
	}

	private Path resolveGitDirectory(List<String> rawCandidates) {
		for (String rawCandidate : rawCandidates) {
			Path candidate = safeToPath(rawCandidate);
			if (candidate == null) {
				continue;
			}

			Path normalizedCandidate = candidate.toAbsolutePath().normalize();
			if (Files.isDirectory(normalizedCandidate) && Files.exists(normalizedCandidate.resolve(".git"))) {
				return normalizedCandidate;
			}
		}

		throw new ApiException(
			ErrorCode.PROJECT_REPOSITORY_NOT_FOUND,
			"No local git repository path is available for the requested project repository."
		);
	}

	private Path safeToPath(String rawPath) {
		if (rawPath == null || rawPath.isBlank()) {
			return null;
		}

		try {
			return Paths.get(rawPath.trim());
		} catch (RuntimeException exception) {
			log.warn("Ignoring invalid repository path candidate: {}", rawPath, exception);
			return null;
		}
	}

	private List<GitCommitSummary> readCommitSummaries(Path repositoryPath, int limit) {
		String output = runRepositoryGitCommand(
			repositoryPath,
			List.of(
				"log",
				"-n",
				String.valueOf(limit),
				"--date=iso-strict",
				"--pretty=format:" + COMMIT_MARKER + "%H" + FIELD_SEPARATOR + "%h" + FIELD_SEPARATOR + "%an"
					+ FIELD_SEPARATOR + "%ae" + FIELD_SEPARATOR + "%ad" + FIELD_SEPARATOR + "%s",
				"--numstat"
			)
		);

		if (output.isBlank()) {
			return List.of();
		}

		List<GitCommitSummary> commits = new ArrayList<>();
		GitCommitSummaryBuilder builder = null;

		for (String rawLine : output.split("\\R")) {
			String line = rawLine == null ? "" : rawLine;
			if (line.startsWith(COMMIT_MARKER)) {
				if (builder != null) {
					commits.add(builder.build());
				}
				builder = GitCommitSummaryBuilder.fromMetadata(line.substring(COMMIT_MARKER.length()));
				continue;
			}

			if (builder == null || line.isBlank()) {
				continue;
			}

			String[] parts = line.split("\\t");
			if (parts.length < 3) {
				continue;
			}

			builder.addFile(parseStatCount(parts[0]), parseStatCount(parts[1]));
		}

		if (builder != null) {
			commits.add(builder.build());
		}

		return commits;
	}

	private GitCommitMetadata readCommitMetadata(Path repositoryPath, String commitHash) {
		String output = runCommitGitCommand(
			repositoryPath,
			commitHash,
			List.of(
				"show",
				"--quiet",
				"--date=iso-strict",
				"--pretty=format:%H" + FIELD_SEPARATOR + "%h" + FIELD_SEPARATOR + "%an" + FIELD_SEPARATOR + "%ae"
					+ FIELD_SEPARATOR + "%ad" + FIELD_SEPARATOR + "%s" + FIELD_SEPARATOR + "%b",
				commitHash
			)
		);

		String[] parts = output.split(FIELD_SEPARATOR, 7);
		if (parts.length < 6) {
			throw new ApiException(ErrorCode.PROJECT_GIT_COMMAND_FAILED, "Failed to parse the project commit metadata.");
		}

		return new GitCommitMetadata(
			parts[0],
			parts[1],
			parts[2],
			parts[3],
			parseGitDateTime(parts[4]),
			parts[5],
			parts.length >= 7 ? parts[6].trim() : ""
		);
	}

	private List<GitCommitFile> readCommitFiles(Path repositoryPath, String commitHash) {
		String output = runCommitGitCommand(
			repositoryPath,
			commitHash,
			List.of(
				"show",
				"--format=",
				"--patch",
				"--find-renames",
				"--unified=3",
				commitHash
			)
		);

		return parseCommitFiles(output);
	}

	private List<GitCommitFile> parseCommitFiles(String rawPatch) {
		if (rawPatch == null || rawPatch.isBlank()) {
			return List.of();
		}

		List<GitCommitFile> files = new ArrayList<>();
		GitCommitFileBuilder builder = null;

		for (String rawLine : rawPatch.split("\\R", -1)) {
			String line = rawLine == null ? "" : rawLine;
			if (line.startsWith("diff --git ")) {
				if (builder != null) {
					files.add(builder.build());
				}
				builder = GitCommitFileBuilder.fromDiffHeader(line);
				continue;
			}

			if (builder == null) {
				continue;
			}

			builder.accept(line);
		}

		if (builder != null) {
			files.add(builder.build());
		}

		return files;
	}

	private String runRepositoryGitCommand(Path repositoryPath, List<String> arguments) {
		try {
			return runGitCommand(repositoryPath, arguments);
		} catch (GitCommandException exception) {
			throw new ApiException(
				ErrorCode.PROJECT_GIT_COMMAND_FAILED,
				"Failed to execute the project git command: " + exception.getOutput()
			);
		}
	}

	private String runCommitGitCommand(Path repositoryPath, String commitHash, List<String> arguments) {
		try {
			return runGitCommand(repositoryPath, arguments);
		} catch (GitCommandException exception) {
			if (isMissingCommitError(exception.getOutput(), commitHash)) {
				throw new ApiException(ErrorCode.PROJECT_COMMIT_NOT_FOUND);
			}

			throw new ApiException(
				ErrorCode.PROJECT_GIT_COMMAND_FAILED,
				"Failed to execute the project git command: " + exception.getOutput()
			);
		}
	}

	private String runGitCommand(Path repositoryPath, List<String> arguments) {
		List<String> command = new ArrayList<>();
		command.add("git");
		command.add("-C");
		command.add(repositoryPath.toString());
		command.addAll(arguments);

		Process process;
		try {
			process = new ProcessBuilder(command)
				.redirectErrorStream(true)
				.start();
		} catch (IOException exception) {
			throw new ApiException(
				ErrorCode.PROJECT_GIT_COMMAND_FAILED,
				"Failed to start the git command. Ensure git is installed on the server host."
			);
		}

		String output;
		try {
			boolean finished = process.waitFor(GIT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				throw new ApiException(ErrorCode.PROJECT_GIT_COMMAND_FAILED, "The git command timed out.");
			}

			output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.PROJECT_GIT_COMMAND_FAILED, "Failed to read the git command output.");
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ApiException(ErrorCode.PROJECT_GIT_COMMAND_FAILED, "The git command was interrupted.");
		}

		if (process.exitValue() != 0) {
			throw new GitCommandException(process.exitValue(), output);
		}

		return output;
	}

	private boolean isMissingCommitError(String output, String commitHash) {
		String normalizedOutput = output == null ? "" : output.toLowerCase(Locale.ROOT);
		String normalizedCommitHash = commitHash == null ? "" : commitHash.toLowerCase(Locale.ROOT);
		return normalizedOutput.contains("unknown revision")
			|| normalizedOutput.contains("bad object")
			|| normalizedOutput.contains("ambiguous argument")
			|| (!normalizedCommitHash.isBlank() && normalizedOutput.contains(normalizedCommitHash) && normalizedOutput.contains("fatal"));
	}

	private long parseStatCount(String rawValue) {
		if (rawValue == null || rawValue.isBlank() || rawValue.equals("-")) {
			return 0L;
		}

		try {
			return Long.parseLong(rawValue.trim());
		} catch (NumberFormatException exception) {
			return 0L;
		}
	}

	private LocalDateTime parseGitDateTime(String rawValue) {
		if (rawValue == null || rawValue.isBlank()) {
			return null;
		}

		try {
			return OffsetDateTime.parse(rawValue.trim()).toLocalDateTime();
		} catch (RuntimeException exception) {
			return LocalDateTime.parse(rawValue.trim());
		}
	}

	private record GitCommitSummary(
		String commitHash,
		String shortCommitHash,
		String message,
		String authorName,
		String authorEmail,
		LocalDateTime committedAt,
		long changedFileCount,
		long additions,
		long deletions
	) {
	}

	private record GitCommitMetadata(
		String commitHash,
		String shortCommitHash,
		String authorName,
		String authorEmail,
		LocalDateTime committedAt,
		String message,
		String body
	) {
	}

	private record GitCommitFile(
		String path,
		String fileName,
		String extension,
		String status,
		long additions,
		long deletions,
		String diff
	) {
	}

	private static final class GitCommitSummaryBuilder {

		private final String commitHash;
		private final String shortCommitHash;
		private final String authorName;
		private final String authorEmail;
		private final LocalDateTime committedAt;
		private final String message;
		private long changedFileCount;
		private long additions;
		private long deletions;

		private GitCommitSummaryBuilder(
			String commitHash,
			String shortCommitHash,
			String authorName,
			String authorEmail,
			LocalDateTime committedAt,
			String message
		) {
			this.commitHash = commitHash;
			this.shortCommitHash = shortCommitHash;
			this.authorName = authorName;
			this.authorEmail = authorEmail;
			this.committedAt = committedAt;
			this.message = message;
		}

		private static GitCommitSummaryBuilder fromMetadata(String metadataLine) {
			String[] parts = metadataLine.split(FIELD_SEPARATOR, 6);
			if (parts.length < 6) {
				throw new ApiException(ErrorCode.PROJECT_GIT_COMMAND_FAILED, "Failed to parse the project commit list.");
			}

			return new GitCommitSummaryBuilder(
				parts[0],
				parts[1],
				parts[2],
				parts[3],
				parseStaticGitDateTime(parts[4]),
				parts[5]
			);
		}

		private void addFile(long additions, long deletions) {
			this.changedFileCount += 1;
			this.additions += additions;
			this.deletions += deletions;
		}

		private GitCommitSummary build() {
			return new GitCommitSummary(
				commitHash,
				shortCommitHash,
				message,
				authorName,
				authorEmail,
				committedAt,
				changedFileCount,
				additions,
				deletions
			);
		}

		private static LocalDateTime parseStaticGitDateTime(String rawValue) {
			if (rawValue == null || rawValue.isBlank()) {
				return null;
			}

			try {
				return OffsetDateTime.parse(rawValue.trim()).toLocalDateTime();
			} catch (RuntimeException exception) {
				return LocalDateTime.parse(rawValue.trim());
			}
		}
	}

	private static final class GitCommitFileBuilder {

		private String oldPath;
		private String currentPath;
		private String status = "MODIFIED";
		private long additions;
		private long deletions;
		private final List<String> diffLines = new ArrayList<>();

		private static GitCommitFileBuilder fromDiffHeader(String diffHeader) {
			GitCommitFileBuilder builder = new GitCommitFileBuilder();
			builder.diffLines.add(diffHeader);

			String header = diffHeader.substring("diff --git ".length());
			int separatorIndex = header.indexOf(" b/");
			if (header.startsWith("a/") && separatorIndex > 1) {
				builder.oldPath = header.substring(2, separatorIndex);
				builder.currentPath = header.substring(separatorIndex + 3);
			} else {
				builder.oldPath = header;
				builder.currentPath = header;
			}

			return builder;
		}

		private void accept(String line) {
			diffLines.add(line);

			if (line.startsWith("new file mode ")) {
				status = "ADDED";
				return;
			}

			if (line.startsWith("deleted file mode ")) {
				status = "DELETED";
				return;
			}

			if (line.startsWith("rename to ")) {
				currentPath = line.substring("rename to ".length()).trim();
				return;
			}

			if (line.startsWith("rename from ")) {
				oldPath = line.substring("rename from ".length()).trim();
				return;
			}

			if (line.startsWith("+++ ") || line.startsWith("--- ")) {
				return;
			}

			if (line.startsWith("+")) {
				additions += 1;
				return;
			}

			if (line.startsWith("-")) {
				deletions += 1;
			}
		}

		private GitCommitFile build() {
			String resolvedPath = switch (status) {
				case "DELETED" -> oldPath;
				default -> currentPath;
			};
			String normalizedPath = resolvedPath == null ? "" : resolvedPath.replace('\\', '/');
			String fileName = normalizedPath.isBlank()
				? ""
				: normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
			String extension = fileName.contains(".")
				? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
				: "";

			return new GitCommitFile(
				normalizedPath,
				fileName,
				extension,
				status,
				additions,
				deletions,
				String.join("\n", diffLines).trim()
			);
		}
	}

	private static final class GitCommandException extends RuntimeException {

		private final int exitCode;
		private final String output;

		private GitCommandException(int exitCode, String output) {
			super(output);
			this.exitCode = exitCode;
			this.output = output == null ? "" : output.trim();
		}

		private int getExitCode() {
			return exitCode;
		}

		private String getOutput() {
			return output;
		}
	}
}
