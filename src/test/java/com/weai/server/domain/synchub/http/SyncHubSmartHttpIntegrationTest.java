package com.weai.server.domain.synchub.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.project.service.ProjectWorkspaceService;
import com.weai.server.domain.synchub.response.SyncHubRepositoryResponse;
import com.weai.server.domain.synchub.service.SyncHubHistoryService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.security.jwt.JwtTokenProvider;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.TransportHttp;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end Git Smart HTTP against the embedded server: real clone/fetch/push over
 * {@code /api/synchub/{projectId}.git} with JGit's client (and the git CLI when it is installed),
 * covering Bearer and Basic auth and the membership/role rules.
 */
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = {
		"app.synchub.root=${java.io.tmpdir}/weai-synchub-it/${random.uuid}",
		"app.workspace.root=${java.io.tmpdir}/weai-workspace-it/${random.uuid}"
	}
)
@ActiveProfiles("test")
class SyncHubSmartHttpIntegrationTest {

	private static final String PASSWORD = "Passw0rd!synchub";

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private SyncHubHistoryService syncHubHistoryService;

	@Autowired
	private ProjectWorkspaceService projectWorkspaceService;

	@TempDir
	Path tempDir;

	private Fixture fixture;

	@BeforeEach
	void setUp() {
		fixture = createFixture();
	}

	@Test
	void unauthenticatedRequestGetsBasicChallenge() throws Exception {
		HttpResponse<String> response = HttpClient.newHttpClient().send(
			HttpRequest.newBuilder(URI.create(remoteUrl() + "/info/refs?service=git-upload-pack")).GET().build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue("WWW-Authenticate")).hasValueSatisfying(value ->
			assertThat(value).startsWith("Basic realm=\"SyncHub\""));
		assertThat(response.body()).doesNotContain("\"success\"");
	}

	@Test
	void infoRefsAdvertisementUsesSmartProtocol() throws Exception {
		HttpResponse<String> response = HttpClient.newHttpClient().send(
			HttpRequest.newBuilder(URI.create(remoteUrl() + "/info/refs?service=git-upload-pack"))
				.header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(fixture.leader()))
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type"))
			.hasValue("application/x-git-upload-pack-advertisement");
		assertThat(response.body()).startsWith("001e# service=git-upload-pack\n0000");
	}

	@Test
	void leaderPushesWithBearerAndMemberClonesAndPushesWithBasic() throws Exception {
		// Leader: clone the (lazily created, empty) repository, commit, push - Bearer token like the app.
		Path leaderWorkTree = tempDir.resolve("leader");
		try (Git git = Git.cloneRepository()
			.setURI(remoteUrl())
			.setDirectory(leaderWorkTree.toFile())
			.setTransportConfigCallback(bearer(fixture.leader()))
			.call()) {
			git.checkout().setOrphan(true).setName("main").call();
			Files.writeString(leaderWorkTree.resolve("README.md"), "hello synchub\n", StandardCharsets.UTF_8);
			Files.createDirectories(leaderWorkTree.resolve("src"));
			Files.writeString(leaderWorkTree.resolve("src/App.java"), "class App {}\n", StandardCharsets.UTF_8);
			git.add().addFilepattern(".").call();
			RevCommit commit = commit(git, "feat: first push");

			assertPushOk(git.push().setRemote("origin").add("main").setTransportConfigCallback(bearer(fixture.leader())).call());

			SyncHubRepositoryResponse info = syncHubHistoryService.getRepositoryInfo(fixture.leader().getEmail(), fixture.project().getId());
			assertThat(info.empty()).isFalse();
			assertThat(info.headCommitHash()).isEqualTo(commit.getName());
			assertThat(info.defaultBranch()).isEqualTo("main");
			assertThat(syncHubHistoryService.getProjectCommits(fixture.leader().getEmail(), fixture.project().getId(), null, null)
				.commits()).hasSize(1);
		}

		// Member: plain CLI-style Basic credentials (email + password).
		Path memberWorkTree = tempDir.resolve("member");
		UsernamePasswordCredentialsProvider memberCredentials =
			new UsernamePasswordCredentialsProvider(fixture.member().getEmail(), PASSWORD);
		try (Git git = Git.cloneRepository()
			.setURI(remoteUrl())
			.setDirectory(memberWorkTree.toFile())
			.setCredentialsProvider(memberCredentials)
			.call()) {
			assertThat(memberWorkTree.resolve("README.md")).hasContent("hello synchub");
			Files.writeString(memberWorkTree.resolve("README.md"), "hello again\n", StandardCharsets.UTF_8);
			git.add().addFilepattern(".").call();
			commit(git, "docs: update readme");
			assertPushOk(git.push().setCredentialsProvider(memberCredentials).call());
		}

		// Wrong password is rejected.
		assertThatThrownBy(() -> Git.cloneRepository()
			.setURI(remoteUrl())
			.setDirectory(tempDir.resolve("wrong").toFile())
			.setCredentialsProvider(new UsernamePasswordCredentialsProvider(fixture.member().getEmail(), "nope"))
			.call()
			.close())
			.hasMessageContaining("not authorized");

		// Builds materialize the pushed default branch.
		Path buildDirectory = projectWorkspaceService.prepareBuildDirectory(fixture.project().getId());
		assertThat(buildDirectory.resolve("README.md")).hasContent("hello again");
		assertThat(buildDirectory.resolve("src/App.java")).exists();
		assertThat(buildDirectory.resolve(".git")).doesNotExist();
	}

	@Test
	// JGit reports an HTTP 403 from the server as "... not permitted on ...".
	void guestCanFetchButNotPushAndOutsiderIsForbidden() throws Exception {
		Path guestWorkTree = tempDir.resolve("guest");
		try (Git git = Git.cloneRepository()
			.setURI(remoteUrl())
			.setDirectory(guestWorkTree.toFile())
			.setTransportConfigCallback(bearer(fixture.guest()))
			.call()) {
			git.checkout().setOrphan(true).setName("main").call();
			Files.writeString(guestWorkTree.resolve("guest.txt"), "guest\n", StandardCharsets.UTF_8);
			git.add().addFilepattern(".").call();
			commit(git, "chore: guest change");

			assertThatThrownBy(() -> git.push().setRemote("origin").add("main").setTransportConfigCallback(bearer(fixture.guest())).call())
				.hasMessageContaining("not permitted");
		}

		assertThatThrownBy(() -> Git.cloneRepository()
			.setURI(remoteUrl())
			.setDirectory(tempDir.resolve("outsider").toFile())
			.setTransportConfigCallback(bearer(fixture.outsider()))
			.call()
			.close())
			.hasMessageContaining("not permitted");
	}

	@Test
	void gitCliClonesAndPushesWithExtraHeader() throws Exception {
		Assumptions.assumeTrue(gitCliAvailable(), "git CLI is not installed");
		String header = "http.extraHeader=Authorization: Bearer " + jwtTokenProvider.createAccessToken(fixture.leader());
		Path workTree = tempDir.resolve("cli");

		runGit(tempDir, "-c", header, "clone", remoteUrl(), workTree.toString());
		Files.writeString(workTree.resolve("cli.txt"), "from cli\n", StandardCharsets.UTF_8);
		runGit(workTree, "checkout", "-b", "main");
		runGit(workTree, "add", ".");
		runGit(workTree, "-c", "user.name=CLI", "-c", "user.email=cli@example.com", "commit", "-m", "feat: cli push");
		runGit(workTree, "-c", header, "push", "origin", "main");

		Path secondClone = tempDir.resolve("cli-second");
		runGit(tempDir, "-c", header, "clone", remoteUrl(), secondClone.toString());
		assertThat(secondClone.resolve("cli.txt")).hasContent("from cli");
	}

	private String remoteUrl() {
		return "http://localhost:" + port + "/api/synchub/" + fixture.project().getId() + ".git";
	}

	private TransportConfigCallback bearer(User user) {
		String token = jwtTokenProvider.createAccessToken(user);
		return transport -> {
			if (transport instanceof TransportHttp http) {
				http.setAdditionalHeaders(Map.of("Authorization", "Bearer " + token));
			}
		};
	}

	private RevCommit commit(Git git, String message) throws Exception {
		PersonIdent ident = new PersonIdent("Tester", "tester@example.com");
		return git.commit().setMessage(message).setAuthor(ident).setCommitter(ident).call();
	}

	private void assertPushOk(Iterable<PushResult> results) {
		for (PushResult result : results) {
			for (RemoteRefUpdate update : result.getRemoteUpdates()) {
				assertThat(update.getStatus()).as(update.getRemoteName() + " " + update.getMessage())
					.isEqualTo(RemoteRefUpdate.Status.OK);
			}
		}
	}

	private boolean gitCliAvailable() {
		try {
			Process process = new ProcessBuilder("git", "--version").redirectErrorStream(true).start();
			return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
		} catch (IOException exception) {
			return false;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private void runGit(Path workingDirectory, String... arguments) throws Exception {
		List<String> command = new ArrayList<>();
		command.add("git");
		command.addAll(List.of(arguments));
		ProcessBuilder builder = new ProcessBuilder(command).directory(workingDirectory.toFile()).redirectErrorStream(true);
		builder.environment().put("GIT_TERMINAL_PROMPT", "0");
		Process process = builder.start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		assertThat(process.waitFor(60, TimeUnit.SECONDS)).isTrue();
		assertThat(process.exitValue()).as("git %s%n%s", command, output).isZero();
	}

	private Fixture createFixture() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User leader = saveUser("sh-leader-" + suffix);
		User member = saveUser("sh-member-" + suffix);
		User guest = saveUser("sh-guest-" + suffix);
		User outsider = saveUser("sh-outsider-" + suffix);
		Project project = projectRepository.save(Project.create(
			"SyncHub " + suffix,
			"SyncHub integration test",
			suffix.toUpperCase(),
			null,
			LocalDate.now(),
			LocalDate.now().plusDays(30),
			leader
		));
		projectMemberRepository.save(ProjectMember.leader(project, leader, ProjectDepartment.BACKEND));
		projectMemberRepository.save(ProjectMember.member(project, member, ProjectDepartment.FRONTEND));
		ProjectMember guestMember = ProjectMember.member(project, guest, ProjectDepartment.FRONTEND);
		guestMember.changeRole(ProjectMemberRole.GUEST);
		projectMemberRepository.save(guestMember);
		projectMemberRepository.flush();
		return new Fixture(project, leader, member, guest, outsider);
	}

	private User saveUser(String username) {
		return userRepository.save(User.create(
			username,
			passwordEncoder.encode(PASSWORD),
			username,
			username + "@example.com",
			UserRole.USER
		));
	}

	private record Fixture(Project project, User leader, User member, User guest, User outsider) {
	}
}
