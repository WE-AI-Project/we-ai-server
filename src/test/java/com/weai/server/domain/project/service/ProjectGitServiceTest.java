package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weai.server.domain.project.config.ProjectGitProperties;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectStatus;
import com.weai.server.domain.project.request.ProjectGitCommitConventionCheckRequest;
import com.weai.server.domain.project.request.ProjectGitCommitRequest;
import com.weai.server.domain.project.response.ProjectGitBranchGraphResponse;
import com.weai.server.domain.project.response.ProjectChangedFileListResponse;
import com.weai.server.domain.project.response.ProjectGitCommitConventionCheckResponse;
import com.weai.server.domain.project.response.ProjectGitChangeResponse;
import com.weai.server.domain.project.response.ProjectGitCommitCreateResponse;
import com.weai.server.domain.project.response.ProjectGitFileDiffResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectGitServiceTest {

	private static final String USER_EMAIL = "roy@example.com";
	private static final Long PROJECT_ID = 1L;
	private static final Long USER_ID = 7L;

	@TempDir
	Path repositoryRoot;

	private ProjectService projectService;
	private UserService userService;
	private ProjectGitService projectGitService;
	private User user;

	@BeforeAll
	static void assumeGitAvailable() throws IOException, InterruptedException {
		Process process = new ProcessBuilder("git", "--version")
			.redirectErrorStream(true)
			.start();

		Assumptions.assumeTrue(process.waitFor() == 0, "git command is required for ProjectGitService tests.");
	}

	@BeforeEach
	void setUp() throws IOException, InterruptedException {
		user = User.builder()
			.id(USER_ID)
			.username("roy")
			.password("password")
			.name("Roy")
			.email(USER_EMAIL)
			.role(UserRole.USER)
			.build();

		projectService = mock(ProjectService.class);
		userService = mock(UserService.class);
		projectGitService = new ProjectGitService(projectService, userService, new ProjectGitProperties());

		when(userService.getUserEntityByEmail(USER_EMAIL)).thenReturn(user);
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID)).thenReturn(project(repositoryRoot));

		initRepository(repositoryRoot);
	}

	@Test
	void getChangedFilesReturnsEmptyListWhenThereAreNoChanges() {
		ProjectChangedFileListResponse response = projectGitService.getChangedFiles(USER_EMAIL, PROJECT_ID);

		assertThat(response.totalChangedCount()).isZero();
		assertThat(response.stagedCount()).isZero();
		assertThat(response.unstagedCount()).isZero();
		assertThat(response.untrackedCount()).isZero();
		assertThat(response.files()).isEmpty();
		assertThat(response.branchName()).isNotBlank();
	}

	@Test
	void getChangedFilesParsesStagedUnstagedAndUntrackedFiles() throws IOException, InterruptedException {
		commitFile(repositoryRoot, "modified.txt", "before");
		Files.writeString(repositoryRoot.resolve("modified.txt"), "after");
		Files.writeString(repositoryRoot.resolve("untracked.txt"), "new");
		Files.writeString(repositoryRoot.resolve("staged.txt"), "new");
		runGit(repositoryRoot, "add", "--", "staged.txt");

		ProjectChangedFileListResponse response = projectGitService.getChangedFiles(USER_EMAIL, PROJECT_ID);

		assertThat(response.totalChangedCount()).isEqualTo(3);
		assertThat(response.stagedCount()).isEqualTo(1);
		assertThat(response.unstagedCount()).isEqualTo(2);
		assertThat(response.untrackedCount()).isEqualTo(1);
		assertThat(response.files()).extracting(ProjectChangedFileListResponse.ChangedFileResponse::filePath)
			.containsExactlyInAnyOrder("modified.txt", "staged.txt", "untracked.txt");
		assertThat(response.files()).anySatisfy(file -> {
			assertThat(file.filePath()).isEqualTo("staged.txt");
			assertThat(file.changeType()).isEqualTo("ADDED");
			assertThat(file.staged()).isTrue();
			assertThat(file.displayStatus()).isEqualTo("STAGED_ADDED");
		});
		assertThat(response.files()).anySatisfy(file -> {
			assertThat(file.filePath()).isEqualTo("untracked.txt");
			assertThat(file.changeType()).isEqualTo("UNTRACKED");
			assertThat(file.unstagedStatus()).isEqualTo("?");
			assertThat(file.displayStatus()).isEqualTo("UNTRACKED");
		});
	}

	@Test
	void getChangedFileDiffReturnsDiffAndStats() throws IOException, InterruptedException {
		commitFile(repositoryRoot, "a.txt", "one\n");
		Files.writeString(repositoryRoot.resolve("a.txt"), "one\ntwo\n");

		ProjectGitFileDiffResponse response = projectGitService.getChangedFileDiff(USER_EMAIL, PROJECT_ID, "a.txt", false);

		assertThat(response.filePath()).isEqualTo("a.txt");
		assertThat(response.staged()).isFalse();
		assertThat(response.changeType()).isEqualTo("MODIFIED");
		assertThat(response.additions()).isEqualTo(1);
		assertThat(response.deletions()).isZero();
		assertThat(response.diffContent()).contains("+two");
	}

	@Test
	void getChangedFileDiffReturnsLargeDiffWithoutTimingOut() throws IOException, InterruptedException {
		commitFile(repositoryRoot, "large.txt", "before\n");
		Files.writeString(repositoryRoot.resolve("large.txt"), "after\n" + "line\n".repeat(30000));

		ProjectGitFileDiffResponse response = projectGitService.getChangedFileDiff(USER_EMAIL, PROJECT_ID, "large.txt", false);

		assertThat(response.additions()).isEqualTo(30001);
		assertThat(response.deletions()).isEqualTo(1);
		assertThat(response.diffContent()).contains("+after");
	}

	@Test
	void getChangedFileDiffFailsWhenFilePathIsMissing() {
		assertThatThrownBy(() -> projectGitService.getChangedFileDiff(USER_EMAIL, PROJECT_ID, " ", false))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GIT_FILE_PATH_REQUIRED));
	}

	@Test
	void getChangedFileDiffFailsWhenFilePathEscapesRepository() {
		assertThatThrownBy(() -> projectGitService.getChangedFileDiff(USER_EMAIL, PROJECT_ID, "../outside.txt", false))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_GIT_FILE_PATH));
	}

	@Test
	void getChangedFileDiffFailsWhenFileIsNotChanged() throws IOException {
		Files.writeString(repositoryRoot.resolve("a.txt"), "new");

		assertThatThrownBy(() -> projectGitService.getChangedFileDiff(USER_EMAIL, PROJECT_ID, "missing.txt", false))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GIT_CHANGED_FILE_NOT_FOUND));
	}

	@Test
	void createCommitCreatesCommitFromStagedFiles() throws IOException, InterruptedException {
		Files.writeString(repositoryRoot.resolve("a.txt"), "hello");
		runGit(repositoryRoot, "add", "--", "a.txt");

		ProjectGitCommitCreateResponse response = projectGitService.createCommit(
			USER_EMAIL,
			PROJECT_ID,
			new ProjectGitCommitRequest("feat: add a file", "commit detail")
		);

		assertThat(response.commitHash()).isNotBlank();
		assertThat(response.shortCommitHash()).hasSize(7);
		assertThat(response.message()).isEqualTo("feat: add a file");
		assertThat(response.committedFileCount()).isEqualTo(1);
		assertThat(response.committedFiles()).containsExactly("a.txt");
		assertThat(cachedFileNames(repositoryRoot)).isEmpty();
		assertThat(runGit(repositoryRoot, "log", "-1", "--pretty=%B")).contains("feat: add a file", "commit detail");
	}

	@Test
	void createCommitFailsWhenMessageIsMissing() throws IOException, InterruptedException {
		Files.writeString(repositoryRoot.resolve("a.txt"), "hello");
		runGit(repositoryRoot, "add", "--", "a.txt");

		assertThatThrownBy(() -> projectGitService.createCommit(
			USER_EMAIL,
			PROJECT_ID,
			new ProjectGitCommitRequest(" ", null)
		))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GIT_COMMIT_MESSAGE_REQUIRED));
	}

	@Test
	void createCommitFailsWhenThereAreNoStagedFiles() {
		assertThatThrownBy(() -> projectGitService.createCommit(
			USER_EMAIL,
			PROJECT_ID,
			new ProjectGitCommitRequest("feat: empty commit", null)
		))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GIT_NO_STAGED_FILES));
	}

	@Test
	void getChangedFilesFailsWhenProjectPathIsNotGitRepository() throws IOException {
		Path nonGitDirectory = Files.createDirectory(repositoryRoot.resolve("not-git"));
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID)).thenReturn(project(nonGitDirectory));

		assertThatThrownBy(() -> projectGitService.getChangedFiles(USER_EMAIL, PROJECT_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GIT_REPOSITORY_NOT_FOUND));
	}

	@Test
	void stageFilesStagesRequestedFiles() throws IOException, InterruptedException {
		Files.writeString(repositoryRoot.resolve("a.txt"), "hello");

		ProjectGitChangeResponse response = projectGitService.stageFiles(USER_EMAIL, PROJECT_ID, List.of("a.txt"));

		assertThat(cachedFileNames(repositoryRoot)).containsExactly("a.txt");
		assertThat(response.filePaths()).containsExactly("a.txt");
		assertThat(response.stagedFiles()).extracting(ProjectGitChangeResponse.GitChangeFileResponse::path)
			.contains("a.txt");
	}

	@Test
	void unstageFilesRemovesRequestedFilesFromStagingArea() throws IOException, InterruptedException {
		commitFile(repositoryRoot, "a.txt", "before");
		Files.writeString(repositoryRoot.resolve("a.txt"), "after");
		runGit(repositoryRoot, "add", "--", "a.txt");

		ProjectGitChangeResponse response = projectGitService.unstageFiles(USER_EMAIL, PROJECT_ID, List.of("a.txt"));

		assertThat(cachedFileNames(repositoryRoot)).isEmpty();
		assertThat(response.filePaths()).containsExactly("a.txt");
		assertThat(response.unstagedFiles()).extracting(ProjectGitChangeResponse.GitChangeFileResponse::path)
			.contains("a.txt");
	}

	@Test
	void stageAllStagesEveryChangedFile() throws IOException, InterruptedException {
		commitFile(repositoryRoot, "a.txt", "before");
		Files.writeString(repositoryRoot.resolve("a.txt"), "after");
		Files.writeString(repositoryRoot.resolve("b.txt"), "new");

		ProjectGitChangeResponse response = projectGitService.stageAll(USER_EMAIL, PROJECT_ID);

		assertThat(cachedFileNames(repositoryRoot)).containsExactlyInAnyOrder("a.txt", "b.txt");
		assertThat(response.stagedAll()).isTrue();
		assertThat(response.stagedFiles()).extracting(ProjectGitChangeResponse.GitChangeFileResponse::path)
			.contains("a.txt", "b.txt");
	}

	@Test
	void unstageAllRemovesEveryStagedFile() throws IOException, InterruptedException {
		commitFile(repositoryRoot, "a.txt", "before");
		Files.writeString(repositoryRoot.resolve("a.txt"), "after");
		Files.writeString(repositoryRoot.resolve("b.txt"), "new");
		runGit(repositoryRoot, "add", "-A");

		ProjectGitChangeResponse response = projectGitService.unstageAll(USER_EMAIL, PROJECT_ID);

		assertThat(cachedFileNames(repositoryRoot)).isEmpty();
		assertThat(response.unstagedAll()).isTrue();
		assertThat(response.unstagedFiles()).extracting(ProjectGitChangeResponse.GitChangeFileResponse::path)
			.contains("a.txt", "b.txt");
	}

	@Test
	void unstageAllSucceedsWhenThereAreNoStagedFiles() throws IOException, InterruptedException {
		Files.writeString(repositoryRoot.resolve("a.txt"), "new");

		ProjectGitChangeResponse response = projectGitService.unstageAll(USER_EMAIL, PROJECT_ID);

		assertThat(cachedFileNames(repositoryRoot)).isEmpty();
		assertThat(response.unstagedAll()).isTrue();
		assertThat(response.stagedFileCount()).isZero();
	}

	@Test
	void stageFilesFailsWhenFilePathsAreEmpty() {
		assertThatThrownBy(() -> projectGitService.stageFiles(USER_EMAIL, PROJECT_ID, List.of()))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GIT_FILE_PATH_REQUIRED));
	}

	@Test
	void stageFilesFailsWhenFilePathIsAbsolute() {
		String absolutePath = repositoryRoot.resolve("a.txt").toAbsolutePath().toString();

		assertThatThrownBy(() -> projectGitService.stageFiles(USER_EMAIL, PROJECT_ID, List.of(absolutePath)))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_GIT_FILE_PATH));
	}

	@Test
	void stageFilesFailsWhenFilePathEscapesRepository() {
		assertThatThrownBy(() -> projectGitService.stageFiles(USER_EMAIL, PROJECT_ID, List.of("../outside.txt")))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_GIT_FILE_PATH));
	}

	@Test
	void stageAllFailsWhenProjectPathIsNotGitRepository() throws IOException {
		Path nonGitDirectory = Files.createDirectory(repositoryRoot.resolve("not-git"));
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID)).thenReturn(project(nonGitDirectory));

		assertThatThrownBy(() -> projectGitService.stageAll(USER_EMAIL, PROJECT_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GIT_REPOSITORY_NOT_FOUND));
	}

	@Test
	void projectAccessFailureIsPropagated() {
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID))
			.thenThrow(new ApiException(ErrorCode.PROJECT_ACCESS_DENIED));

		assertThatThrownBy(() -> projectGitService.stageAll(USER_EMAIL, PROJECT_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED));
	}

	@Test
	void missingProjectFailureIsPropagated() {
		when(projectService.validateProjectAccess(PROJECT_ID, USER_ID))
			.thenThrow(new ApiException(ErrorCode.PROJECT_NOT_FOUND));

		assertThatThrownBy(() -> projectGitService.stageAll(USER_EMAIL, PROJECT_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void checkCommitConventionAcceptsConventionalCommitMessages() {
		ProjectGitCommitConventionCheckResponse response = projectGitService.checkCommitConvention(
			USER_EMAIL,
			PROJECT_ID,
			new ProjectGitCommitConventionCheckRequest("feat(chat): 채팅방 생성 API 구현", "상세 설명")
		);

		assertThat(response.valid()).isTrue();
		assertThat(response.type()).isEqualTo("feat");
		assertThat(response.scope()).isEqualTo("chat");
		assertThat(response.subject()).isEqualTo("채팅방 생성 API 구현");
		assertThat(response.normalizedMessage()).isEqualTo("feat(chat): 채팅방 생성 API 구현");
	}

	@Test
	void checkCommitConventionReturnsInvalidResultForUnsupportedType() {
		ProjectGitCommitConventionCheckResponse response = projectGitService.checkCommitConvention(
			USER_EMAIL,
			PROJECT_ID,
			new ProjectGitCommitConventionCheckRequest("feature: 새 기능", null)
		);

		assertThat(response.valid()).isFalse();
		assertThat(response.errors()).extracting(ProjectGitCommitConventionCheckResponse.ConventionIssue::code)
			.contains("UNSUPPORTED_COMMIT_TYPE");
	}

	@Test
	void getBranchGraphReturnsCommitNodesAndParentEdges() throws IOException, InterruptedException {
		commitFile(repositoryRoot, "first.txt", "first");
		commitFile(repositoryRoot, "second.txt", "second");

		ProjectGitBranchGraphResponse response = projectGitService.getBranchGraph(
			USER_EMAIL, PROJECT_ID, null, 50, true
		);

		assertThat(response.currentBranch()).isNotBlank();
		assertThat(response.nodes()).hasSize(2);
		assertThat(response.edges()).hasSize(1);
		assertThat(response.nodes().get(0).branchNames()).isNotEmpty();
	}

	@Test
	void getBranchGraphRejectsInvalidLimitAndDangerousBranchName() {
		assertThatThrownBy(() -> projectGitService.getBranchGraph(USER_EMAIL, PROJECT_ID, null, 201, true))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_GIT_GRAPH_LIMIT));
		assertThatThrownBy(() -> projectGitService.getBranchGraph(USER_EMAIL, PROJECT_ID, "main;whoami", 50, true))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_GIT_BRANCH_NAME));
	}

	private Project project(Path localPath) {
		return Project.builder()
			.id(PROJECT_ID)
			.projectName("Project")
			.projectCode("ABCDEFG1")
			.localPath(localPath.toString())
			.status(ProjectStatus.ACTIVE)
			.createdBy(user)
			.build();
	}

	private void initRepository(Path repositoryPath) throws IOException, InterruptedException {
		runGit(repositoryPath, "init");
		runGit(repositoryPath, "config", "user.email", "test@example.com");
		runGit(repositoryPath, "config", "user.name", "Test User");
	}

	private void commitFile(Path repositoryPath, String filePath, String content) throws IOException, InterruptedException {
		Path targetPath = repositoryPath.resolve(filePath);
		Files.createDirectories(targetPath.getParent() == null ? repositoryPath : targetPath.getParent());
		Files.writeString(targetPath, content);
		runGit(repositoryPath, "add", "--", filePath);
		runGit(repositoryPath, "commit", "-m", "initial commit");
	}

	private List<String> cachedFileNames(Path repositoryPath) throws IOException, InterruptedException {
		String output = runGit(repositoryPath, "diff", "--cached", "--name-only");
		if (output.isBlank()) {
			return List.of();
		}
		return output.lines().toList();
	}

	private String runGit(Path repositoryPath, String... arguments) throws IOException, InterruptedException {
		List<String> command = new java.util.ArrayList<>();
		command.add("git");
		command.add("-C");
		command.add(repositoryPath.toString());
		command.addAll(List.of(arguments));

		Process process = new ProcessBuilder(command)
			.redirectErrorStream(true)
			.start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		int exitCode = process.waitFor();
		assertThat(exitCode)
			.as("git command failed: %s%n%s", command, output)
			.isZero();
		return output;
	}
}
