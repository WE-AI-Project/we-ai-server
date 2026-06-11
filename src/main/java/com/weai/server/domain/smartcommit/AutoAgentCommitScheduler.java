package com.weai.server.domain.smartcommit;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AutoAgentCommitScheduler {

	private static final Logger log = LoggerFactory.getLogger(AutoAgentCommitScheduler.class);
	private static final String AUTO_AGENT_COMMIT = "AUTO_AGENT_COMMIT";

	private static final String SYSTEM_PROMPT = """
		You are SYNAIPSE's no-git smart commit agent.
		Read accumulated file diffs and create one useful auto commit.
		Return a concise result in this exact shape:
		Commit message: <semantic commit style message>
		Summary: <what changed and why>
		Do not invent files that are not in the diff.
		""";

	private final PendingDiffStore pendingDiffStore;
	private final OllamaChatModel smartCommitModel;
	private final Duration idleThreshold;
	private final Duration commitCooldown;

	public AutoAgentCommitScheduler(
		PendingDiffStore pendingDiffStore,
		@Qualifier("debateLlamaChatModel") OllamaChatModel smartCommitModel,
		@Value("${smart-commit.idle-threshold:PT10M}") Duration idleThreshold,
		@Value("${smart-commit.commit-cooldown:PT5M}") Duration commitCooldown
	) {
		this.pendingDiffStore = pendingDiffStore;
		this.smartCommitModel = smartCommitModel;
		this.idleThreshold = idleThreshold;
		this.commitCooldown = commitCooldown;
	}

	@Scheduled(fixedDelayString = "${smart-commit.scheduler.fixed-delay-ms:60000}")
	public void createAutoCommitWhenIdle() {
		Instant now = Instant.now();
		pendingDiffStore.drainForAutoCommit(now, idleThreshold, commitCooldown)
			.ifPresent(this::createAutoAgentCommit);
	}

	private void createAutoAgentCommit(PendingDiffStore.PendingCommitBatch batch) {
		try {
			String aiResult = generateCommitAnalysis(batch.combinedDiff());
			PendingDiffStore.AutoAgentCommit commit = new PendingDiffStore.AutoAgentCommit(
				AUTO_AGENT_COMMIT,
				extractCommitMessage(aiResult),
				aiResult,
				batch.diffs().size(),
				Instant.now()
			);
			pendingDiffStore.recordAutoAgentCommit(commit);
			log.info(
				"Created {} with {} pending file(s): {}",
				AUTO_AGENT_COMMIT,
				commit.changedFileCount(),
				commit.message()
			);
		} catch (RuntimeException exception) {
			pendingDiffStore.restore(batch);
			log.warn("Failed to create auto agent commit. Restored pending diffs.", exception);
		}
	}

	private String generateCommitAnalysis(String combinedDiff) {
		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from(SYSTEM_PROMPT));
		messages.add(UserMessage.from("""
			Create one AUTO_AGENT_COMMIT for the accumulated pending diffs below.

			Diffs:
			%s
			""".formatted(combinedDiff)));

		String response = smartCommitModel.generate(messages).content().text();
		if (!StringUtils.hasText(response)) {
			throw new IllegalStateException("The smart commit model returned an empty response.");
		}
		return response.trim();
	}

	private String extractCommitMessage(String aiResult) {
		for (String line : aiResult.split("\\R")) {
			if (line.toLowerCase().startsWith("commit message:")) {
				String message = line.substring("commit message:".length()).trim();
				if (StringUtils.hasText(message)) {
					return message;
				}
			}
		}
		return "chore: auto commit pending workspace changes";
	}
}
