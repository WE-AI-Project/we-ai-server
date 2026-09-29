package com.weai.server.domain.smartcommit;

import com.weai.server.domain.smartcommit.domain.SynCommitType;
import com.weai.server.domain.smartcommit.service.SynCommitAiService;
import com.weai.server.domain.smartcommit.service.SynCommitService;
import com.weai.server.domain.smartcommit.service.SynCommitService.PendingCommitBatch;
import com.weai.server.global.logging.ProjectLogContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Automatically folds a project's idle "syn add" batch into a "syn commit" once nobody has staged
 * a new diff for {@code idleThreshold} and the previous syn commit is older than {@code commitCooldown}.
 *
 * If the AI call keeps failing (model down, bad response, etc.), this used to restore the pending
 * changes and immediately become eligible to retry on the very next {@code fixedDelay} tick (60s by
 * default) - forever, with no backoff, burning one AI call per project per tick indefinitely.
 * {@link #failureBackoff} now tracks consecutive failures per project and skips a project until an
 * exponentially growing cooldown elapses, capped at {@link #MAX_BACKOFF}.
 */
@Component
public class AutoAgentCommitScheduler {

	private static final Logger log = LoggerFactory.getLogger(AutoAgentCommitScheduler.class);
	private static final Duration MAX_BACKOFF = Duration.ofMinutes(30);

	private final SynCommitService synCommitService;
	private final SynCommitAiService synCommitAiService;
	private final Duration idleThreshold;
	private final Duration commitCooldown;
	private final Map<Long, FailureBackoff> failureBackoff = new ConcurrentHashMap<>();

	public AutoAgentCommitScheduler(
		SynCommitService synCommitService,
		SynCommitAiService synCommitAiService,
		@Value("${smart-commit.idle-threshold:PT10M}") Duration idleThreshold,
		@Value("${smart-commit.commit-cooldown:PT5M}") Duration commitCooldown
	) {
		this.synCommitService = synCommitService;
		this.synCommitAiService = synCommitAiService;
		this.idleThreshold = idleThreshold;
		this.commitCooldown = commitCooldown;
	}

	@Scheduled(fixedDelayString = "${smart-commit.scheduler.fixed-delay-ms:60000}")
	public void createAutoCommitWhenIdle() {
		Instant now = Instant.now();
		synCommitService.drainAllReadyForAutoCommit(now, idleThreshold, commitCooldown)
			.forEach(batch -> createAutoAgentCommit(batch, now));
	}

	private void createAutoAgentCommit(PendingCommitBatch batch, Instant now) {
		FailureBackoff backoff = failureBackoff.get(batch.projectId());
		if (backoff != null && backoff.retryAt().isAfter(now)) {
			// Still cooling down from a previous failure - put the batch straight back as pending
			// rather than spending another AI call we expect to fail the same way.
			synCommitService.restorePendingChanges(batch.projectId(), batch.changes());
			return;
		}

		try (ProjectLogContext.Scope ignored = ProjectLogContext.open(batch.projectId(), "AGENT")) {
			SynCommitAiService.GeneratedSynCommit generated = synCommitAiService.generate(batch.projectId(), batch.combinedDiff());
			var commit = synCommitService.recordSynCommit(
				batch.projectId(),
				SynCommitType.AUTO,
				generated.commitMessage(),
				generated.summary(),
				batch.changes()
			);
			failureBackoff.remove(batch.projectId());
			log.info(
				"Created syn commit {} with {} staged file(s): {}",
				commit.synCommitId(),
				commit.changedFileCount(),
				commit.commitMessage()
			);
		} catch (RuntimeException exception) {
			synCommitService.restorePendingChanges(batch.projectId(), batch.changes());
			FailureBackoff nextBackoff = failureBackoff
				.computeIfAbsent(batch.projectId(), ignored -> new FailureBackoff(0, Instant.EPOCH))
				.next(now);
			failureBackoff.put(batch.projectId(), nextBackoff);
			log.warn(
				"Failed to create auto syn commit for projectId={} (consecutive failure #{}). Restored staged changes; "
					+ "will not retry before {}.",
				batch.projectId(), nextBackoff.consecutiveFailures(), nextBackoff.retryAt(), exception
			);
		}
	}

	private record FailureBackoff(int consecutiveFailures, Instant retryAt) {
		FailureBackoff next(Instant now) {
			int failures = consecutiveFailures + 1;
			Duration exponentialDelay = Duration.ofMinutes(1L << Math.min(failures, 10));
			Duration delay = exponentialDelay.compareTo(MAX_BACKOFF) < 0 ? exponentialDelay : MAX_BACKOFF;
			return new FailureBackoff(failures, now.plus(delay));
		}
	}
}
