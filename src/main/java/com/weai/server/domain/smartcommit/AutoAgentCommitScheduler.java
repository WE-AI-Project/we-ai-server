package com.weai.server.domain.smartcommit;

import com.weai.server.domain.smartcommit.domain.SynCommitType;
import com.weai.server.domain.smartcommit.service.SynCommitAiService;
import com.weai.server.domain.smartcommit.service.SynCommitService;
import com.weai.server.domain.smartcommit.service.SynCommitService.PendingCommitBatch;
import com.weai.server.global.logging.ProjectLogContext;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Automatically folds a project's idle "syn add" batch into a "syn commit" once nobody has staged
 * a new diff for {@code idleThreshold} and the previous syn commit is older than {@code commitCooldown}.
 */
@Component
public class AutoAgentCommitScheduler {

	private static final Logger log = LoggerFactory.getLogger(AutoAgentCommitScheduler.class);

	private final SynCommitService synCommitService;
	private final SynCommitAiService synCommitAiService;
	private final Duration idleThreshold;
	private final Duration commitCooldown;

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
			.forEach(this::createAutoAgentCommit);
	}

	private void createAutoAgentCommit(PendingCommitBatch batch) {
		try (ProjectLogContext.Scope ignored = ProjectLogContext.open(batch.projectId(), "AGENT")) {
			SynCommitAiService.GeneratedSynCommit generated = synCommitAiService.generate(batch.projectId(), batch.combinedDiff());
			var commit = synCommitService.recordSynCommit(
				batch.projectId(),
				SynCommitType.AUTO,
				generated.commitMessage(),
				generated.summary(),
				batch.changes()
			);
			log.info(
				"Created syn commit {} with {} staged file(s): {}",
				commit.synCommitId(),
				commit.changedFileCount(),
				commit.commitMessage()
			);
		} catch (RuntimeException exception) {
			synCommitService.restorePendingChanges(batch.projectId(), batch.changes());
			log.warn("Failed to create auto syn commit. Restored staged changes.", exception);
		}
	}
}
