package com.weai.server.domain.smartcommit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PendingDiffStore {

	private static final int MAX_RECORDED_COMMITS = 100;

	private final Object stateLock = new Object();
	private final ConcurrentHashMap<Long, ProjectPendingState> projectStates = new ConcurrentHashMap<>();

	public void addOrUpdate(Long projectId, String filePath, String diff) {
		if (projectId == null || !StringUtils.hasText(filePath) || !StringUtils.hasText(diff)) {
			return;
		}

		Instant now = Instant.now();
		synchronized (stateLock) {
			ProjectPendingState state = projectStates.computeIfAbsent(projectId, ignored -> new ProjectPendingState());
			state.pendingDiffs.put(filePath, new PendingDiff(filePath, diff, now));
			state.lastModifiedTime = now;
		}
	}

	public List<PendingCommitBatch> drainAllReadyForAutoCommit(
		Instant now,
		Duration idleThreshold,
		Duration commitCooldown
	) {
		synchronized (stateLock) {
			List<PendingCommitBatch> batches = new ArrayList<>();
			projectStates.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> drainForAutoCommit(entry.getKey(), entry.getValue(), now, idleThreshold, commitCooldown)
					.ifPresent(batches::add));
			return List.copyOf(batches);
		}
	}

	public Optional<PendingCommitBatch> drainForManualCommit(Long projectId, Instant now, Duration commitCooldown) {
		synchronized (stateLock) {
			ProjectPendingState state = projectStates.get(projectId);
			if (state == null || state.pendingDiffs.isEmpty()) {
				return Optional.empty();
			}
			return drainIfCooldownElapsed(projectId, state, now, commitCooldown);
		}
	}

	public void restore(PendingCommitBatch batch) {
		synchronized (stateLock) {
			ProjectPendingState state = projectStates.computeIfAbsent(batch.projectId(), ignored -> new ProjectPendingState());
			for (PendingDiff diff : batch.diffs()) {
				state.pendingDiffs.merge(diff.filePath(), diff, this::mergeRestoredDiff);
			}
			state.lastModifiedTime = state.pendingDiffs.values().stream()
				.map(PendingDiff::modifiedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);
		}
	}

	public void recordAutoAgentCommit(Long projectId, AutoAgentCommit commit) {
		synchronized (stateLock) {
			ProjectPendingState state = projectStates.computeIfAbsent(projectId, ignored -> new ProjectPendingState());
			state.recentCommits.add(commit);
			if (state.recentCommits.size() > MAX_RECORDED_COMMITS) {
				state.recentCommits.remove(0);
			}
		}
	}

	public PendingStateSnapshot snapshot(Long projectId) {
		synchronized (stateLock) {
			ProjectPendingState state = projectStates.get(projectId);
			if (state == null) {
				return new PendingStateSnapshot(0, null, Instant.EPOCH, List.of());
			}
			return new PendingStateSnapshot(
				state.pendingDiffs.size(),
				state.lastModifiedTime,
				state.lastCommitTime,
				List.copyOf(state.recentCommits)
			);
		}
	}

	private Optional<PendingCommitBatch> drainForAutoCommit(
		Long projectId,
		ProjectPendingState state,
		Instant now,
		Duration idleThreshold,
		Duration commitCooldown
	) {
		if (state.pendingDiffs.isEmpty() || state.lastModifiedTime == null) {
			return Optional.empty();
		}
		if (Duration.between(state.lastModifiedTime, now).compareTo(idleThreshold) < 0) {
			return Optional.empty();
		}
		return drainIfCooldownElapsed(projectId, state, now, commitCooldown);
	}

	private Optional<PendingCommitBatch> drainIfCooldownElapsed(
		Long projectId,
		ProjectPendingState state,
		Instant now,
		Duration commitCooldown
	) {
		if (Duration.between(state.lastCommitTime, now).compareTo(commitCooldown) < 0) {
			return Optional.empty();
		}

		Map<String, PendingDiff> drainedDiffs = new LinkedHashMap<>();
		state.pendingDiffs.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> drainedDiffs.put(entry.getKey(), entry.getValue()));

		state.pendingDiffs.clear();
		state.lastModifiedTime = null;
		state.lastCommitTime = now;
		return Optional.of(new PendingCommitBatch(projectId, now, List.copyOf(drainedDiffs.values())));
	}

	private PendingDiff mergeRestoredDiff(PendingDiff current, PendingDiff restored) {
		String mergedDiff = restored.diff() + "\n\n--- newer pending diff after failed commit ---\n\n" + current.diff();
		Instant modifiedAt = current.modifiedAt().isAfter(restored.modifiedAt())
			? current.modifiedAt()
			: restored.modifiedAt();
		return new PendingDiff(current.filePath(), mergedDiff, modifiedAt);
	}

	private static final class ProjectPendingState {
		private final Map<String, PendingDiff> pendingDiffs = new LinkedHashMap<>();
		private final List<AutoAgentCommit> recentCommits = new ArrayList<>();
		private Instant lastModifiedTime;
		private Instant lastCommitTime = Instant.EPOCH;
	}

	public record PendingDiff(String filePath, String diff, Instant modifiedAt) {
	}

	public record PendingCommitBatch(Long projectId, Instant drainedAt, List<PendingDiff> diffs) {
		public String combinedDiff() {
			StringBuilder builder = new StringBuilder();
			for (PendingDiff diff : diffs) {
				builder.append("File: ").append(diff.filePath()).append('\n')
					.append(diff.diff()).append("\n\n");
			}
			return builder.toString().trim();
		}
	}

	public record AutoAgentCommit(
		String type,
		String message,
		String summary,
		int changedFileCount,
		Instant committedAt
	) {
	}

	public record PendingStateSnapshot(
		int pendingFileCount,
		Instant lastModifiedTime,
		Instant lastCommitTime,
		List<AutoAgentCommit> recentCommits
	) {
	}
}
