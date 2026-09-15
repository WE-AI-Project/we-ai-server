package com.weai.server.domain.smartcommit.service;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.smartcommit.domain.SynCommit;
import com.weai.server.domain.smartcommit.domain.SynCommitFile;
import com.weai.server.domain.smartcommit.domain.SynCommitType;
import com.weai.server.domain.smartcommit.domain.SynPendingChange;
import com.weai.server.domain.smartcommit.repository.SynCommitFileRepository;
import com.weai.server.domain.smartcommit.repository.SynCommitRepository;
import com.weai.server.domain.smartcommit.repository.SynPendingChangeRepository;
import com.weai.server.domain.smartcommit.response.SynCommitListResponse;
import com.weai.server.domain.smartcommit.response.SynCommitResponse;
import com.weai.server.domain.smartcommit.SmartCommitPendingResponse;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * DB-backed replacement for the old in-memory {@code PendingDiffStore}: "syn add" stages a file
 * diff ({@link SynPendingChange}), and "syn commit" folds the staged batch into a persisted
 * {@link SynCommit}. Because everything lives in the database, staged changes and commit history
 * survive an app restart and are shared across every server instance — unlike the previous
 * {@code ConcurrentHashMap}-based store.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SynCommitService {

	private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final SynPendingChangeRepository synPendingChangeRepository;
	private final SynCommitRepository synCommitRepository;
	private final SynCommitFileRepository synCommitFileRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;

	/** "syn add": stage or update one file's diff for a project. */
	@Transactional
	public SmartCommitPendingResponse registerPendingChange(Long projectId, Long userId, String filePath, String diff) {
		if (projectId == null || !StringUtils.hasText(filePath) || !StringUtils.hasText(diff)) {
			return getPendingSnapshot(projectId);
		}

		synPendingChangeRepository.findByProject_IdAndFilePath(projectId, filePath)
			.ifPresentOrElse(
				existing -> existing.updateDiff(userRepository.getReferenceById(userId), diff),
				() -> synPendingChangeRepository.save(SynPendingChange.create(
					projectRepository.getReferenceById(projectId),
					userRepository.getReferenceById(userId),
					filePath,
					diff
				))
			);

		return getPendingSnapshot(projectId);
	}

	public SmartCommitPendingResponse getPendingSnapshot(Long projectId) {
		List<SynPendingChange> pending = synPendingChangeRepository.findByProject_IdOrderByFilePathAsc(projectId);
		Instant lastModifiedTime = pending.stream()
			.map(change -> toInstant(change.getUpdatedAt()))
			.max(Comparator.naturalOrder())
			.orElse(null);
		return new SmartCommitPendingResponse(pending.size(), lastModifiedTime);
	}

	/** "syn commit" triggered on demand. Throws if there is nothing staged or the cooldown has not elapsed. */
	@Transactional
	public PendingCommitBatch drainForManualCommit(Long projectId, Instant now, Duration commitCooldown) {
		List<SynPendingChange> pending = synPendingChangeRepository.findByProject_IdOrderByFilePathAsc(projectId);
		if (pending.isEmpty()) {
			throw new ApiException(ErrorCode.SYN_COMMIT_NOTHING_PENDING);
		}
		if (Duration.between(lastCommitTime(projectId), now).compareTo(commitCooldown) < 0) {
			throw new ApiException(ErrorCode.SYN_COMMIT_COOLDOWN_ACTIVE);
		}
		return drain(projectId, pending, now);
	}

	/** "syn commit" triggered automatically once a project's staged changes have gone idle. */
	@Transactional
	public List<PendingCommitBatch> drainAllReadyForAutoCommit(Instant now, Duration idleThreshold, Duration commitCooldown) {
		List<PendingCommitBatch> batches = new ArrayList<>();
		for (Long projectId : synPendingChangeRepository.findDistinctProjectIds()) {
			List<SynPendingChange> pending = synPendingChangeRepository.findByProject_IdOrderByFilePathAsc(projectId);
			if (pending.isEmpty()) {
				continue;
			}
			Instant lastModifiedTime = pending.stream()
				.map(change -> toInstant(change.getUpdatedAt()))
				.max(Comparator.naturalOrder())
				.orElse(null);
			if (lastModifiedTime == null || Duration.between(lastModifiedTime, now).compareTo(idleThreshold) < 0) {
				continue;
			}
			if (Duration.between(lastCommitTime(projectId), now).compareTo(commitCooldown) < 0) {
				continue;
			}
			batches.add(drain(projectId, pending, now));
		}
		return List.copyOf(batches);
	}

	/** Restores staged changes that could not be turned into a syn commit (e.g. the AI call failed). */
	@Transactional
	public void restorePendingChanges(Long projectId, List<PendingChangeData> changes) {
		Project project = projectRepository.getReferenceById(projectId);
		for (PendingChangeData change : changes) {
			synPendingChangeRepository.findByProject_IdAndFilePath(projectId, change.filePath())
				.ifPresentOrElse(
					existing -> existing.mergeAfterFailedCommit(change.diffContent()),
					() -> synPendingChangeRepository.save(SynPendingChange.create(
						project,
						userRepository.getReferenceById(change.registeredByUserId()),
						change.filePath(),
						change.diffContent()
					))
				);
		}
	}

	@Transactional
	public SynCommitResponse recordSynCommit(
		Long projectId,
		SynCommitType type,
		String commitMessage,
		String summary,
		List<PendingChangeData> changes
	) {
		Project project = projectRepository.getReferenceById(projectId);
		SynCommit commit = synCommitRepository.save(SynCommit.create(
			project,
			type,
			commitMessage,
			summary,
			changes.size(),
			LocalDateTime.now()
		));

		List<SynCommitFile> files = synCommitFileRepository.saveAll(
			changes.stream().map(change -> SynCommitFile.create(commit, change.filePath(), change.diffContent())).toList()
		);

		return SynCommitResponse.from(commit, files);
	}

	public SynCommitListResponse getSynCommits(Long projectId, Integer page, Integer size) {
		Pageable pageable = normalizePageable(page, size);
		Page<SynCommit> commits = synCommitRepository.findByProject_Id(projectId, pageable);
		return SynCommitListResponse.from(projectId, commits);
	}

	public SynCommitResponse getSynCommitDetail(Long projectId, Long synCommitId) {
		SynCommit commit = synCommitRepository.findByIdAndProject_Id(synCommitId, projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.SYN_COMMIT_NOT_FOUND));
		List<SynCommitFile> files = synCommitFileRepository.findBySynCommit_IdOrderByFilePathAsc(synCommitId);
		return SynCommitResponse.from(commit, files);
	}

	private PendingCommitBatch drain(Long projectId, List<SynPendingChange> pending, Instant now) {
		List<PendingChangeData> changes = pending.stream()
			.map(change -> new PendingChangeData(change.getFilePath(), change.getDiffContent(), change.getRegisteredBy().getId()))
			.toList();
		synPendingChangeRepository.deleteAll(pending);
		return new PendingCommitBatch(projectId, now, changes);
	}

	private Instant lastCommitTime(Long projectId) {
		return synCommitRepository.findTopByProject_IdOrderByCommittedAtDesc(projectId)
			.map(commit -> toInstant(commit.getCommittedAt()))
			.orElse(Instant.EPOCH);
	}

	private Instant toInstant(LocalDateTime dateTime) {
		return dateTime.atZone(SYSTEM_ZONE).toInstant();
	}

	private Pageable normalizePageable(Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		if (resolvedPage < 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "page must be greater than or equal to 0.");
		}
		if (resolvedSize <= 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "size must be greater than 0.");
		}
		return PageRequest.of(
			resolvedPage,
			Math.min(resolvedSize, MAX_SIZE),
			Sort.by(Sort.Direction.DESC, "committedAt").and(Sort.by(Sort.Direction.DESC, "id"))
		);
	}

	public record PendingChangeData(String filePath, String diffContent, Long registeredByUserId) {
	}

	public record PendingCommitBatch(Long projectId, Instant drainedAt, List<PendingChangeData> changes) {

		public String combinedDiff() {
			StringBuilder builder = new StringBuilder();
			for (PendingChangeData change : changes) {
				builder.append("File: ").append(change.filePath()).append('\n')
					.append(change.diffContent()).append("\n\n");
			}
			return builder.toString().trim();
		}
	}
}
