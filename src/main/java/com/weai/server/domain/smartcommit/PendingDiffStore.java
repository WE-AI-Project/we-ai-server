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
	private final ConcurrentHashMap<String, PendingDiff> pendingDiffs = new ConcurrentHashMap<>();
	private final List<AutoAgentCommit> recentCommits = new ArrayList<>();
	private Instant lastModifiedTime;
	private Instant lastCommitTime = Instant.EPOCH;

	public void addOrUpdate(String filePath, String diff) {
		if (!StringUtils.hasText(filePath) || !StringUtils.hasText(diff)) {
			return;
		}

		Instant now = Instant.now();
		// File-save events and the scheduler can run on different threads.
		// This lock keeps diff updates and lastModifiedTime changes atomic.
		synchronized (stateLock) {
			pendingDiffs.put(filePath, new PendingDiff(filePath, diff, now));
			lastModifiedTime = now;
		}
	}

	public Optional<PendingCommitBatch> drainForAutoCommit(
		Instant now,
		Duration idleThreshold,
		Duration commitCooldown
	) {
		synchronized (stateLock) {
			if (pendingDiffs.isEmpty() || lastModifiedTime == null) {
				return Optional.empty();
			}
			if (Duration.between(lastModifiedTime, now).compareTo(idleThreshold) < 0) {
				return Optional.empty();
			}
			return drainIfCooldownElapsed(now, commitCooldown);
		}
	}

	public Optional<PendingCommitBatch> drainForManualCommit(Instant now, Duration commitCooldown) {
		synchronized (stateLock) {
			if (pendingDiffs.isEmpty()) {
				return Optional.empty();
			}
			return drainIfCooldownElapsed(now, commitCooldown);
		}
	}

	public void restore(PendingCommitBatch batch) {
		synchronized (stateLock) {
			for (PendingDiff diff : batch.diffs()) {
				pendingDiffs.merge(diff.filePath(), diff, this::mergeRestoredDiff);
			}
			lastModifiedTime = pendingDiffs.values().stream()
				.map(PendingDiff::modifiedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);
		}
	}

	public void recordAutoAgentCommit(AutoAgentCommit commit) {
		synchronized (stateLock) {
			recentCommits.add(commit);
			if (recentCommits.size() > MAX_RECORDED_COMMITS) {
				recentCommits.remove(0);
			}
		}
	}

	public PendingStateSnapshot snapshot() {
		synchronized (stateLock) {
			return new PendingStateSnapshot(
				pendingDiffs.size(),
				lastModifiedTime,
				lastCommitTime,
				List.copyOf(recentCommits)
			);
		}
	}

	private Optional<PendingCommitBatch> drainIfCooldownElapsed(Instant now, Duration commitCooldown) {
		if (Duration.between(lastCommitTime, now).compareTo(commitCooldown) < 0) {
			return Optional.empty();
		}

		// Copy and clear happen inside the same critical section. A new Ctrl+S event
		// either lands before this drain and is included, or waits and becomes a fresh
		// pending diff after the drain. That avoids losing updates during auto commit.
		Map<String, PendingDiff> drainedDiffs = new LinkedHashMap<>();
		pendingDiffs.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> drainedDiffs.put(entry.getKey(), entry.getValue()));

		pendingDiffs.clear();
		lastModifiedTime = null;
		lastCommitTime = now;
		return Optional.of(new PendingCommitBatch(now, List.copyOf(drainedDiffs.values())));
	}

	private PendingDiff mergeRestoredDiff(PendingDiff current, PendingDiff restored) {
		// If AI commit generation fails after draining, newer saves may already exist.
		// Merge the restored old diff before the newer diff instead of overwriting it.
		String mergedDiff = restored.diff() + "\n\n--- newer pending diff after failed commit ---\n\n" + current.diff();
		Instant modifiedAt = current.modifiedAt().isAfter(restored.modifiedAt())
			? current.modifiedAt()
			: restored.modifiedAt();
		return new PendingDiff(current.filePath(), mergedDiff, modifiedAt);
	}

	public record PendingDiff(
		String filePath,
		String diff,
		Instant modifiedAt
	) {
	}

	public record PendingCommitBatch(
		Instant drainedAt,
		List<PendingDiff> diffs
	) {
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
