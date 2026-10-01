package com.weai.server.domain.ai.rag;

import com.weai.server.domain.project.service.ProjectWorkspaceService;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

/**
 * Indexes every eligible text file of a project's uploaded workspace into RAG, and removes indexed
 * workspace files that no longer exist. Runs off the request thread; at most one run per project at
 * a time - a trigger that arrives mid-run schedules exactly one follow-up run.
 */
@Slf4j
@Component
public class WorkspaceRagIndexer {

	static final int MAX_FILES = 1_500;

	private final ProjectWorkspaceService projectWorkspaceService;
	private final ProjectRagIndexService projectRagIndexService;
	private final Executor ragIndexExecutor;

	private final Set<Long> running = ConcurrentHashMap.newKeySet();
	private final Set<Long> rerunRequested = ConcurrentHashMap.newKeySet();
	private final Map<Long, WorkspaceIndexStatus> statuses = new ConcurrentHashMap<>();

	public WorkspaceRagIndexer(
		ProjectWorkspaceService projectWorkspaceService,
		ProjectRagIndexService projectRagIndexService,
		@Qualifier("ragIndexExecutor") Executor ragIndexExecutor
	) {
		this.projectWorkspaceService = projectWorkspaceService;
		this.projectRagIndexService = projectRagIndexService;
		this.ragIndexExecutor = ragIndexExecutor;
	}

	public void schedule(Long projectId) {
		if (!running.add(projectId)) {
			rerunRequested.add(projectId);
			return;
		}
		statuses.put(projectId, WorkspaceIndexStatus.queued());
		try {
			ragIndexExecutor.execute(() -> runUntilSettled(projectId));
		} catch (TaskRejectedException exception) {
			running.remove(projectId);
			statuses.put(projectId, WorkspaceIndexStatus.failed(LocalDateTime.now(), "Indexing queue is full; try again later."));
			log.warn("Workspace RAG indexing rejected for projectId={}: executor queue full.", projectId);
		}
	}

	public Optional<WorkspaceIndexStatus> status(Long projectId) {
		return Optional.ofNullable(statuses.get(projectId));
	}

	private void runUntilSettled(Long projectId) {
		try {
			do {
				rerunRequested.remove(projectId);
				statuses.put(projectId, run(projectId));
			} while (rerunRequested.contains(projectId));
		} finally {
			running.remove(projectId);
			if (rerunRequested.remove(projectId)) {
				schedule(projectId);
			}
		}
	}

	WorkspaceIndexStatus run(Long projectId) {
		LocalDateTime startedAt = LocalDateTime.now();
		Path root = projectWorkspaceService.resolveProjectDirectory(projectId);
		if (!Files.isDirectory(root)) {
			return WorkspaceIndexStatus.failed(startedAt, "No workspace has been uploaded for this project.");
		}

		List<Path> files;
		try {
			files = collectFiles(root);
		} catch (IOException exception) {
			log.warn("Failed to scan workspace for projectId={}", projectId, exception);
			return WorkspaceIndexStatus.failed(startedAt, "Failed to scan workspace: " + exception.getMessage());
		}

		Set<String> currentSources = new HashSet<>();
		int indexed = 0;
		int unchanged = 0;
		int failed = 0;
		for (Path file : files) {
			String source = root.relativize(file).toString().replace('\\', '/');
			Optional<String> text = readText(file);
			if (text.isEmpty() || text.get().isBlank()) {
				continue;
			}
			currentSources.add(source);
			try {
				RagDocumentIndexResponse response = projectRagIndexService.index(projectId, source, text.get(), RagDocumentOrigin.WORKSPACE);
				if (response.unchanged()) {
					unchanged++;
				} else {
					indexed++;
				}
			} catch (RuntimeException exception) {
				failed++;
				log.warn("Failed to index workspace file projectId={}, source={}", projectId, source, exception);
			}
		}

		int removed = 0;
		for (RagDocument document : projectRagIndexService.listByOrigin(projectId, RagDocumentOrigin.WORKSPACE)) {
			if (!currentSources.contains(document.getSource())) {
				projectRagIndexService.delete(projectId, document.getSource());
				removed++;
			}
		}

		log.info(
			"Workspace RAG indexing finished for projectId={}: {} indexed, {} unchanged, {} removed, {} failed",
			projectId, indexed, unchanged, removed, failed
		);
		return WorkspaceIndexStatus.completed(startedAt, LocalDateTime.now(), indexed, unchanged, removed, failed);
	}

	private List<Path> collectFiles(Path root) throws IOException {
		List<Path> files = new ArrayList<>();
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				if (!dir.equals(root) && RagSourcePolicy.isSkippedDirectory(dir.getFileName().toString())) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (files.size() >= MAX_FILES) {
					return FileVisitResult.TERMINATE;
				}
				if (attrs.isRegularFile()
					&& attrs.size() <= RagSourcePolicy.MAX_FILE_BYTES
					&& RagSourcePolicy.isIndexableWorkspaceFile(file.getFileName().toString())) {
					files.add(file);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exception) {
				return FileVisitResult.CONTINUE;
			}
		});
		return files;
	}

	// Skips binary or non-UTF-8 files instead of indexing mojibake.
	private Optional<String> readText(Path file) {
		try {
			byte[] bytes = Files.readAllBytes(file);
			for (int i = 0; i < Math.min(bytes.length, 8_192); i++) {
				if (bytes[i] == 0) {
					return Optional.empty();
				}
			}
			return Optional.of(StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(ByteBuffer.wrap(bytes))
				.toString());
		} catch (CharacterCodingException exception) {
			return Optional.empty();
		} catch (IOException exception) {
			log.debug("Unreadable workspace file {}", file, exception);
			return Optional.empty();
		}
	}

	public record WorkspaceIndexStatus(
		String state,
		LocalDateTime startedAt,
		LocalDateTime finishedAt,
		int indexedFiles,
		int unchangedFiles,
		int removedFiles,
		int failedFiles,
		String error
	) {
		static WorkspaceIndexStatus queued() {
			return new WorkspaceIndexStatus("RUNNING", LocalDateTime.now(), null, 0, 0, 0, 0, null);
		}

		static WorkspaceIndexStatus completed(LocalDateTime startedAt, LocalDateTime finishedAt, int indexed, int unchanged, int removed, int failed) {
			return new WorkspaceIndexStatus("COMPLETED", startedAt, finishedAt, indexed, unchanged, removed, failed, null);
		}

		static WorkspaceIndexStatus failed(LocalDateTime startedAt, String error) {
			return new WorkspaceIndexStatus("FAILED", startedAt, LocalDateTime.now(), 0, 0, 0, 0, error);
		}
	}
}
