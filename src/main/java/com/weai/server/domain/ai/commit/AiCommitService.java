package com.weai.server.domain.ai.commit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class AiCommitService {

	private static final String SYSTEM_PROMPT = """
		You are SYNAIPSE's RAG-grounded commit message generator.
		Use the supplied project-isolated official document context as the authority for architecture, naming, and conventions.
		Return exactly one JSON object. Do not include markdown or code fences.
		Schema:
		{
		  "candidates": [
		    {"message":"...","type":"fix","scope":"ai","title":"...","body":"..."}
		  ]
		}
		Generate 1 to 3 useful conventional commit candidates.
		Do not invent files or behavior that are not in the diff or project context.
		""";

	private final ObjectMapper objectMapper;
	private final OllamaChatModel commitModel;
	private final ProjectRagContextService projectRagContextService;

	public AiCommitService(
		@Qualifier("commitJsonChatModel") OllamaChatModel commitModel,
		ProjectRagContextService projectRagContextService
	) {
		this.objectMapper = new ObjectMapper();
		this.commitModel = commitModel;
		this.projectRagContextService = projectRagContextService;
	}

	public AiCommitResponse generate(Long projectId, String diff, List<String> files) {
		if (projectId == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "projectId is required.");
		}
		if (!StringUtils.hasText(diff)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "diff is required.");
		}

		ProjectRagContext ragContext = projectRagContextService.retrieve(projectId, buildRagQuery(diff.trim(), files));
		if (ragContext.isEmpty()) {
			throw new ApiException(
				ErrorCode.INVALID_INPUT,
				"No project RAG context was found for this commit request. Index project documents before generating AI commit messages."
			);
		}

		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from(SYSTEM_PROMPT));
		messages.add(UserMessage.from(buildPrompt(projectId, diff.trim(), files, ragContext.formatted())));

		String rawJson = commitModel.chat(messages).aiMessage().text();
		if (!StringUtils.hasText(rawJson)) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "The AI commit model returned an empty response.");
		}

		try {
			JsonNode root = objectMapper.readTree(rawJson);
			JsonNode candidatesNode = root.get("candidates");
			if (candidatesNode == null || !candidatesNode.isArray() || candidatesNode.isEmpty()) {
				throw new IllegalArgumentException("Missing candidates.");
			}

			List<AiCommitResponse.Candidate> candidates = new ArrayList<>();
			for (JsonNode candidateNode : candidatesNode) {
				String message = readRequiredText(candidateNode, "message");
				candidates.add(new AiCommitResponse.Candidate(
					message,
					message,
					readOptionalText(candidateNode, "type"),
					readOptionalText(candidateNode, "scope"),
					readOptionalText(candidateNode, "title"),
					readOptionalText(candidateNode, "body")
				));
			}

			String primary = candidates.get(0).message();
			return new AiCommitResponse(primary, primary, List.copyOf(candidates));
		} catch (Exception exception) {
			throw new ApiException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"Failed to parse the AI commit response as JSON."
			);
		}
	}

	private String buildRagQuery(String diff, List<String> files) {
		return """
			Commit message generation request.
			Files:
			%s

			Diff:
			%s
			""".formatted(formatFiles(files), diff);
	}

	private String buildPrompt(Long projectId, String diff, List<String> files, String ragContext) {
		return """
			Project ID: %d

			Project-isolated official document context:
			%s

			Changed files:
			%s

			Diff:
			%s

			Instructions:
			- Use only the diff and the project context above.
			- Prefer conventional commits.
			- Keep the first candidate as the best default.
			""".formatted(projectId, ragContext, formatFiles(files), diff);
	}

	private String formatFiles(List<String> files) {
		if (files == null || files.isEmpty()) {
			return "(not supplied)";
		}
		return String.join("\n", files);
	}

	private String readRequiredText(JsonNode root, String fieldName) {
		String value = readOptionalText(root, fieldName);
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException("Missing required field: " + fieldName);
		}
		return value;
	}

	private String readOptionalText(JsonNode root, String fieldName) {
		JsonNode node = root.get(fieldName);
		if (node == null || !node.isTextual()) {
			return "";
		}
		return node.asText().trim();
	}
}
