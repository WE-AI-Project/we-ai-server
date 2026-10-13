package com.weai.server.domain.synchub.service;

import com.weai.server.domain.synchub.config.SyncHubProperties;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;
import org.springframework.stereotype.Service;

/**
 * Storage layer for SyncHub: locates, lazily creates and opens the bare repository of each project
 * ({@code {app.synchub.root}/{projectId}.git}) with JGit, so the server never depends on a git CLI
 * being installed and never touches a client machine's path.
 *
 * Callers own the returned {@link Repository} and must close it.
 */
@Slf4j
@Service
public class SyncHubRepositoryService {

	private final SyncHubProperties properties;
	private final Path root;
	private final ConcurrentMap<Long, Object> initLocks = new ConcurrentHashMap<>();

	public SyncHubRepositoryService(SyncHubProperties properties) {
		this.properties = properties;
		this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
	}

	public Path resolveRepositoryPath(Long projectId) {
		return root.resolve(projectId + Constants.DOT_GIT_EXT).normalize();
	}

	public boolean exists(Long projectId) {
		return RepositoryCache.FileKey.isGitRepository(resolveRepositoryPath(projectId).toFile(), FS.DETECTED);
	}

	public Optional<Repository> openIfExists(Long projectId) {
		if (!exists(projectId)) {
			return Optional.empty();
		}
		return Optional.of(open(projectId));
	}

	/** Opens the project's bare repository, creating it (HEAD -> the configured default branch) on first use. */
	public Repository openOrCreate(Long projectId) {
		if (!exists(projectId)) {
			synchronized (initLocks.computeIfAbsent(projectId, ignored -> new Object())) {
				if (!exists(projectId)) {
					initialize(projectId);
				}
			}
		}
		return open(projectId);
	}

	public String getDefaultBranchSetting() {
		return properties.getDefaultBranch();
	}

	public List<Ref> listBranches(Repository repository) {
		try {
			return repository.getRefDatabase().getRefsByPrefix(Constants.R_HEADS).stream()
				.filter(ref -> ref.getObjectId() != null)
				.sorted(Comparator.comparing(Ref::getName))
				.toList();
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED, exception.getMessage(), exception);
		}
	}

	/**
	 * The branch builds and the commit list follow: HEAD's target when it exists, otherwise the
	 * configured default branch, otherwise the first branch (e.g. a client that pushed only
	 * {@code master}). Empty when the repository has no branches at all.
	 */
	public Optional<Ref> resolveDefaultBranch(Repository repository) {
		List<Ref> branches = listBranches(repository);
		if (branches.isEmpty()) {
			return Optional.empty();
		}

		String headTarget = readHeadTarget(repository);
		String configured = Constants.R_HEADS + properties.getDefaultBranch();
		return branches.stream().filter(ref -> ref.getName().equals(headTarget)).findFirst()
			.or(() -> branches.stream().filter(ref -> ref.getName().equals(configured)).findFirst())
			.or(() -> Optional.of(branches.get(0)));
	}

	/** Name shown as the repository's default branch, even before anything has been pushed. */
	public String resolveDefaultBranchName(Repository repository) {
		return resolveDefaultBranch(repository)
			.map(ref -> Repository.shortenRefName(ref.getName()))
			.orElseGet(() -> {
				String headTarget = readHeadTarget(repository);
				return headTarget != null && headTarget.startsWith(Constants.R_HEADS)
					? Repository.shortenRefName(headTarget)
					: properties.getDefaultBranch();
			});
	}

	/**
	 * After a push: when HEAD points at a branch that does not exist (first push used another branch
	 * name than the default), re-point HEAD at an existing branch so clones check something out.
	 */
	public void repairDanglingHead(Repository repository) {
		try {
			Ref head = repository.exactRef(Constants.HEAD);
			if (head != null && head.getObjectId() != null) {
				return;
			}
			Optional<Ref> fallback = resolveDefaultBranch(repository);
			if (fallback.isEmpty()) {
				return;
			}
			RefUpdate update = repository.updateRef(Constants.HEAD);
			update.disableRefLog();
			update.link(fallback.get().getName());
			log.info("SyncHub HEAD re-pointed to {} in {}", fallback.get().getName(), repository.getDirectory());
		} catch (IOException exception) {
			log.warn("Failed to repair SyncHub HEAD in {}", repository.getDirectory(), exception);
		}
	}

	/** Commit hash at the tip of the default branch, if the project's repository has any commits. */
	public Optional<String> findDefaultHeadCommit(Long projectId) {
		Optional<Repository> repository = openIfExists(projectId);
		if (repository.isEmpty()) {
			return Optional.empty();
		}
		try (Repository opened = repository.get()) {
			return resolveDefaultBranch(opened).map(ref -> ref.getObjectId().name());
		}
	}

	/**
	 * Writes the tree of {@code commitHash} into {@code targetDirectory} (which should be empty) as
	 * plain files - no .git directory. Executable bits are kept (gradlew/mvnw must stay runnable);
	 * submodules are skipped and symlinks are written as regular files holding their target path.
	 *
	 * @return number of files written
	 */
	public int exportCommit(Long projectId, String commitHash, Path targetDirectory) {
		Path normalizedTarget = targetDirectory.toAbsolutePath().normalize();
		try (Repository repository = open(projectId);
			RevWalk revWalk = new RevWalk(repository);
			ObjectReader reader = repository.newObjectReader();
			TreeWalk treeWalk = new TreeWalk(repository, reader)) {
			RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitHash));
			treeWalk.addTree(commit.getTree());
			treeWalk.setRecursive(true);

			Files.createDirectories(normalizedTarget);
			int written = 0;
			while (treeWalk.next()) {
				FileMode mode = treeWalk.getFileMode(0);
				if (mode == FileMode.GITLINK) {
					continue;
				}
				Path file = normalizedTarget.resolve(treeWalk.getPathString()).normalize();
				if (!file.startsWith(normalizedTarget)) {
					throw new ApiException(ErrorCode.SYNCHUB_EXPORT_FAILED, "Unsafe path in repository: " + treeWalk.getPathString());
				}
				Files.createDirectories(file.getParent());
				try (OutputStream outputStream = Files.newOutputStream(file)) {
					reader.open(treeWalk.getObjectId(0)).copyTo(outputStream);
				}
				if (mode == FileMode.EXECUTABLE_FILE) {
					file.toFile().setExecutable(true, false);
				}
				written++;
			}
			return written;
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.SYNCHUB_EXPORT_FAILED, exception.getMessage(), exception);
		}
	}

	private Repository open(Long projectId) {
		try {
			return new FileRepositoryBuilder()
				.setGitDir(resolveRepositoryPath(projectId).toFile())
				.setMustExist(true)
				.build();
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED, exception.getMessage(), exception);
		}
	}

	private void initialize(Long projectId) {
		Path repositoryPath = resolveRepositoryPath(projectId);
		try {
			Files.createDirectories(root);
			try (Git ignored = Git.init()
				.setBare(true)
				.setGitDir(repositoryPath.toFile())
				.setInitialBranch(properties.getDefaultBranch())
				.call()) {
				log.info("Created SyncHub repository for projectId={} at {}", projectId, repositoryPath);
			}
		} catch (IOException | GitAPIException exception) {
			throw new ApiException(ErrorCode.SYNCHUB_REPOSITORY_INIT_FAILED, exception.getMessage(), exception);
		}
	}

	private String readHeadTarget(Repository repository) {
		try {
			Ref head = repository.exactRef(Constants.HEAD);
			return head == null ? null : head.getTarget().getName();
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED, exception.getMessage(), exception);
		}
	}
}
