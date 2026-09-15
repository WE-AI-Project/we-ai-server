package com.weai.server.domain.smartcommit.service;

import com.weai.server.domain.ai.rag.ProjectRagContext;
import com.weai.server.domain.ai.rag.ProjectRagContextService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * RAG-grounded generator behind a "syn commit": summarizes a batch of "syn add"-staged diffs into
 * a commit message and a human-readable summary. Used by both {@link SynCommitService}'s manual
 * trigger path and {@code AutoAgentCommitScheduler}'s idle-batch path.
 */
@Service
public class SynCommitAiService {

	private static final String SYSTEM_PROMPT = """
		You are SYNAIPSE's syn-commit agent: a no-git, DB-backed alternative to `git commit`.
		Read the accumulated file diffs staged via "syn add" and create one useful syn commit.
		Return a concise result in this exact shape:
		Commit message: <semantic commit style message>
		Summary: <what changed and why>
		Do not invent files that are not in the diff.
		""";

	private final ProjectRagContextService projectRagContextService;
	private final OllamaChatModel synCommitModel;

	public SynCommitAiService(
		ProjectRagContextService projectRagContextService,
		@Qualifier("debateLlamaChatModel") OllamaChatModel synCommitModel
	) {
		this.projectRagContextService = projectRagContextService;
		this.synCommitModel = synCommitModel;
	}

	public GeneratedSynCommit generate(Long projectId, String combinedDiff) {
		ProjectRagContext ragContext = projectRagContextService.retrieve(projectId, buildRagQuery(combinedDiff));
		if (ragContext.isEmpty()) {
			throw new ApiException(
				ErrorCode.SYN_COMMIT_GENERATION_FAILED,
				"No project RAG context was found for syn commit projectId=" + projectId + "."
			);
		}

		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from(SYSTEM_PROMPT));
		messages.add(UserMessage.from("""
			Project ID: %d

			Project-isolated official document context:
			%s

			Create one syn commit for the accumulated staged diffs below.
			Use the project context above as the authority for conventions, architecture, and naming.

			Diffs:
			%s
			""".formatted(projectId, ragContext.formatted(), combinedDiff)));

		String response = synCommitModel.chat(messages).aiMessage().text();
		if (!StringUtils.hasText(response)) {
			throw new ApiException(ErrorCode.SYN_COMMIT_GENERATION_FAILED, "The syn commit model returned an empty response.");
		}

		String trimmedResponse = response.trim();
		return new GeneratedSynCommit(extractCommitMessage(trimmedResponse), trimmedResponse);
	}

	private String buildRagQuery(String combinedDiff) {
		return """
			Syn commit generation request.

			Diffs:
			%s
			""".formatted(combinedDiff);
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
		return "chore: syn commit staged workspace changes";
	}

	public record GeneratedSynCommit(String commitMessage, String summary) {
	}
}
