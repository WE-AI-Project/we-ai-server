package com.weai.server.domain.synchub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weai.server.domain.project.domain.ProjectRepositoryType;
import com.weai.server.domain.project.request.ProjectGitCommitConventionCheckRequest;
import com.weai.server.domain.project.response.ProjectCommitDetailResponse;
import com.weai.server.domain.project.response.ProjectCommitFileDiffResponse;
import com.weai.server.domain.project.response.ProjectCommitFileListResponse;
import com.weai.server.domain.project.response.ProjectCommitListResponse;
import com.weai.server.domain.project.response.ProjectGitBranchGraphResponse;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.project.service.ProjectWorkspaceService;
import com.weai.server.domain.synchub.config.SyncHubProperties;
import com.weai.server.domain.synchub.response.SyncHubRepositoryResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.config.AppWebProperties;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.storage.ObjectStorageService;
import com.weai.server.global.storage.StorageProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyncHubHistoryServiceTest {

	private static final String USER_EMAIL = "roy@example.com";
	private static final Long PROJECT_ID = 22L;
	private static final Long USER_ID = 7L;

	@TempDir
	Path tempDir;

	private SyncHubRepositoryService repositoryService;
	private SyncHubHistoryService historyService;

	@BeforeEach
	void setUp() {
		SyncHubProperties properties = new SyncHubProperties();
		properties.setRoot(tempDir.resolve("synchub").toString());
		repositoryService = new SyncHubRepositoryService(properties);

		AppWebProperties appWebProperties = new AppWebProperties();
		appWebProperties.setBaseUrl("https://api.yhy-server.com/");

		User user = User.builder()
			.id(USER_ID)
			.username("roy")
			.password("password")
			.name("Roy")
			.email(USER_EMAIL)
			.role(UserRole.USER)
			.build();
		UserService userService = mock(UserService.class);
		ProjectService projectService = mock(ProjectService.class);
		when(userService.getUserEntityByEmail(USER_EMAIL)).thenReturn(user);

		historyService = new SyncHubHistoryService(projectService, userService, repositoryService, properties, appWebProperties);
	}

	@Test
	void uninitializedRepositoryReturnsEmptyResults() {
		ProjectCommitListResponse commits = historyService.getProjectCommits(USER_EMAIL, PROJECT_ID, null, null);
		ProjectGitBranchGraphResponse graph = historyService.getBranchGraph(USER_EMAIL, PROJECT_ID, null, null, true);
		SyncHubRepositoryResponse info = historyService.getRepositoryInfo(USER_EMAIL, PROJECT_ID);

		assertThat(commits.commits()).isEmpty();
		assertThat(commits.repositoryType()).isEqualTo(ProjectRepositoryType.BACKEND);
		assertThat(graph.nodes()).isEmpty();
		assertThat(graph.branches()).isEmpty();
		assertThat(info.initialized()).isFalse();
		assertThat(info.empty()).isTrue();
		assertThat(info.remoteUrl()).isEqualTo("https://api.yhy-server.com/api/synchub/22.git");
		assertThat(info.remoteName()).isEqualTo("synchub");
		assertThat(info.defaultBranch()).isEqualTo("main");
		assertThat(repositoryService.exists(PROJECT_ID)).isFalse();
	}

	@Test
	void emptyBareRepositoryReturnsEmptyResults() {
		repositoryService.openOrCreate(PROJECT_ID).close();

		ProjectCommitListResponse commits = historyService.getProjectCommits(USER_EMAIL, PROJECT_ID, "FRONTEND", 10);
		ProjectGitBranchGraphResponse graph = historyService.getBranchGraph(USER_EMAIL, PROJECT_ID, null, 10, true);
		SyncHubRepositoryResponse info = historyService.getRepositoryInfo(USER_EMAIL, PROJECT_ID);

		assertThat(commits.commits()).isEmpty();
		assertThat(commits.repositoryType()).isEqualTo(ProjectRepositoryType.FRONTEND);
		assertThat(graph.nodes()).isEmpty();
		assertThat(graph.currentBranch()).isEqualTo("main");
		assertThat(info.initialized()).isTrue();
		assertThat(info.empty()).isTrue();
		assertThat(info.headCommitHash()).isNull();
		assertThatThrownBy(() -> historyService.getProjectCommitDetail(USER_EMAIL, PROJECT_ID, null, "abcdef1"))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_COMMIT_NOT_FOUND);
	}

	@Test
	void readsCommitsDetailFilesDiffAndBranchGraph() throws Exception {
		Fixture fixture = pushFixtureHistory();

		ProjectCommitListResponse commits = historyService.getProjectCommits(USER_EMAIL, PROJECT_ID, "INVALID", 20);
		assertThat(commits.commits()).extracting(ProjectCommitListResponse.CommitSummaryResponse::message)
			.containsExactly("feat: add service", "chore: initial commit");
		ProjectCommitListResponse.CommitSummaryResponse latest = commits.commits().get(0);
		assertThat(latest.commitHash()).isEqualTo(fixture.second().getName());
		assertThat(latest.shortCommitHash()).hasSize(7);
		assertThat(latest.authorEmail()).isEqualTo("roy@example.com");
		assertThat(latest.changedFileCount()).isEqualTo(3);
		assertThat(latest.additions()).isEqualTo(2);
		assertThat(latest.deletions()).isEqualTo(2);

		ProjectCommitDetailResponse detail = historyService.getProjectCommitDetail(
			USER_EMAIL, PROJECT_ID, null, fixture.second().getName().substring(0, 10)
		);
		assertThat(detail.commitHash()).isEqualTo(fixture.second().getName());
		assertThat(detail.message()).isEqualTo("feat: add service");
		assertThat(detail.body()).isEqualTo("Adds the service class.");
		assertThat(detail.changedFileCount()).isEqualTo(3);

		ProjectCommitFileListResponse files = historyService.getProjectCommitFiles(
			USER_EMAIL, PROJECT_ID, null, fixture.second().getName()
		);
		assertThat(files.files())
			.extracting(ProjectCommitFileListResponse.CommitFileResponse::path, ProjectCommitFileListResponse.CommitFileResponse::status)
			.containsExactlyInAnyOrder(
				org.assertj.core.groups.Tuple.tuple("README.md", "MODIFIED"),
				org.assertj.core.groups.Tuple.tuple("src/Service.java", "ADDED"),
				org.assertj.core.groups.Tuple.tuple("obsolete.txt", "DELETED")
			);
		ProjectCommitFileListResponse.CommitFileResponse service = files.files().stream()
			.filter(file -> file.path().equals("src/Service.java"))
			.findFirst()
			.orElseThrow();
		assertThat(service.fileName()).isEqualTo("Service.java");
		assertThat(service.extension()).isEqualTo("java");

		ProjectCommitFileDiffResponse diff = historyService.getProjectCommitFileDiff(
			USER_EMAIL, PROJECT_ID, null, fixture.second().getName(), "README.md"
		);
		assertThat(diff.status()).isEqualTo("MODIFIED");
		assertThat(diff.additions()).isEqualTo(1);
		assertThat(diff.deletions()).isEqualTo(1);
		assertThat(diff.diff())
			.startsWith("diff --git a/README.md b/README.md")
			.contains("-hello", "+hello world");

		assertThatThrownBy(() -> historyService.getProjectCommitFileDiff(
			USER_EMAIL, PROJECT_ID, null, fixture.second().getName(), "missing.txt"
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_COMMIT_FILE_NOT_FOUND);
		assertThatThrownBy(() -> historyService.getProjectCommitDetail(USER_EMAIL, PROJECT_ID, null, "HEAD~1"))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_COMMIT_NOT_FOUND);

		ProjectGitBranchGraphResponse graph = historyService.getBranchGraph(USER_EMAIL, PROJECT_ID, null, 50, true);
		assertThat(graph.currentBranch()).isEqualTo("main");
		assertThat(graph.branches()).extracting(ProjectGitBranchGraphResponse.BranchResponse::name)
			.containsExactly("feature/x", "main");
		assertThat(graph.nodes()).hasSize(3);
		assertThat(graph.nodes().get(0).commitHash()).isEqualTo(fixture.feature().getName());
		assertThat(graph.nodes().get(0).branchNames()).containsExactly("feature/x");
		ProjectGitBranchGraphResponse.CommitNodeResponse root = graph.nodes().get(2);
		assertThat(root.commitHash()).isEqualTo(fixture.first().getName());
		assertThat(root.branchNames()).containsExactlyInAnyOrder("feature/x", "main");
		assertThat(graph.edges()).hasSize(2)
			.allSatisfy(edge -> assertThat(edge.from()).isEqualTo(fixture.first().getName()));

		ProjectGitBranchGraphResponse mainOnly = historyService.getBranchGraph(USER_EMAIL, PROJECT_ID, "main", 50, false);
		assertThat(mainOnly.nodes()).hasSize(2);
		assertThatThrownBy(() -> historyService.getBranchGraph(USER_EMAIL, PROJECT_ID, "nope", 50, true))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.GIT_BRANCH_NOT_FOUND);

		SyncHubRepositoryResponse info = historyService.getRepositoryInfo(USER_EMAIL, PROJECT_ID);
		assertThat(info.empty()).isFalse();
		assertThat(info.headCommitHash()).isEqualTo(fixture.second().getName());
		assertThat(info.branches()).containsExactly("feature/x", "main");
	}

	@Test
	void prepareBuildDirectoryExportsDefaultBranchHead() throws Exception {
		Fixture fixture = pushFixtureHistory();
		Path workspaceRoot = tempDir.resolve("workspace");
		ProjectWorkspaceService workspaceService = new ProjectWorkspaceService(
			mock(ObjectStorageService.class),
			new StorageProperties(),
			repositoryService,
			workspaceRoot.toString()
		);

		Path directory = workspaceService.prepareBuildDirectory(PROJECT_ID);

		assertThat(directory).isEqualTo(workspaceRoot.resolve(String.valueOf(PROJECT_ID)).toAbsolutePath().normalize());
		assertThat(Files.readString(directory.resolve("README.md"), StandardCharsets.UTF_8)).isEqualTo("hello world\n");
		assertThat(directory.resolve("src/Service.java")).exists();
		assertThat(directory.resolve("obsolete.txt")).doesNotExist();
		assertThat(directory.resolve(".git")).doesNotExist();
		assertThat(fixture.second()).isNotNull();
	}

	@Test
	void prepareBuildDirectoryWithoutCommitsOrSnapshotTellsUserToPush() {
		ProjectWorkspaceService workspaceService = new ProjectWorkspaceService(
			mock(ObjectStorageService.class),
			new StorageProperties(),
			repositoryService,
			tempDir.resolve("workspace").toString()
		);
		repositoryService.openOrCreate(PROJECT_ID).close();

		assertThatThrownBy(() -> workspaceService.prepareBuildDirectory(PROJECT_ID))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_WORKSPACE_NOT_FOUND);
		assertThat(ErrorCode.PROJECT_WORKSPACE_NOT_FOUND.getMessage()).contains("푸시");
	}

	@Test
	void commitConventionCheckStillWorks() {
		var valid = historyService.checkCommitConvention(
			USER_EMAIL, PROJECT_ID, new ProjectGitCommitConventionCheckRequest("feat(chat): 채팅방 생성", null)
		);
		var invalid = historyService.checkCommitConvention(
			USER_EMAIL, PROJECT_ID, new ProjectGitCommitConventionCheckRequest("added stuff", null)
		);

		assertThat(valid.valid()).isTrue();
		assertThat(valid.normalizedMessage()).isEqualTo("feat(chat): 채팅방 생성");
		assertThat(invalid.valid()).isFalse();
		assertThat(invalid.errors()).extracting("code").containsExactly("INVALID_COMMIT_MESSAGE_FORMAT");
	}

	/** main: initial -> second, feature/x: initial -> feature. Pushed from a working repository, like the app does. */
	private Fixture pushFixtureHistory() throws Exception {
		repositoryService.openOrCreate(PROJECT_ID).close();
		Path workTree = tempDir.resolve("clone");
		Instant now = Instant.now();
		try (Git git = Git.init().setDirectory(workTree.toFile()).setInitialBranch("main").call()) {
			write(workTree.resolve("README.md"), "hello\n");
			write(workTree.resolve("obsolete.txt"), "old\n");
			git.add().addFilepattern(".").call();
			RevCommit first = commit(git, "chore: initial commit", now.minusSeconds(300));

			git.branchCreate().setName("feature/x").call();

			write(workTree.resolve("README.md"), "hello world\n");
			write(workTree.resolve("src/Service.java"), "class Service {}\n");
			git.rm().addFilepattern("obsolete.txt").call();
			git.add().addFilepattern(".").call();
			RevCommit second = commit(git, "feat: add service\n\nAdds the service class.\n", now.minusSeconds(200));

			git.checkout().setName("feature/x").call();
			write(workTree.resolve("feature.txt"), "feature\n");
			git.add().addFilepattern(".").call();
			RevCommit feature = commit(git, "feat: feature work", now.minusSeconds(100));

			git.push()
				.setRemote(repositoryService.resolveRepositoryPath(PROJECT_ID).toUri().toString())
				.setRefSpecs(new RefSpec("refs/heads/*:refs/heads/*"))
				.call();
			return new Fixture(first, second, feature);
		}
	}

	private RevCommit commit(Git git, String message, Instant when) throws Exception {
		PersonIdent ident = new PersonIdent("Roy", "roy@example.com", when, ZoneId.of("Asia/Seoul"));
		return git.commit().setMessage(message).setAuthor(ident).setCommitter(ident).call();
	}

	private void write(Path file, String content) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, content, StandardCharsets.UTF_8);
	}

	private record Fixture(RevCommit first, RevCommit second, RevCommit feature) {
	}
}
