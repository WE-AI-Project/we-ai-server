package com.weai.server.domain.ai.chat;

import com.weai.server.domain.ai.rag.ProjectRagRetriever;
import com.weai.server.domain.ai.rag.ThinkingLevel;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiChatService {

	private static final String BASE_SYSTEM_PROMPT = """
		You are Oracle, SYNAIPSE's internal project knowledge assistant.
		Prefer the supplied project-isolated document context when it is relevant to the question.
		If the context is missing, empty, or insufficient to answer, do NOT refuse and do NOT say
		you don't know - answer using your own general knowledge instead, and briefly note (in one
		short sentence) that project-specific documentation was limited for this question.
		Never invent specific project APIs, tables, config values, or architecture details that are
		not present in the context - general knowledge is fine, fabricated project internals are not.
		Write in Korean, but keep code identifiers and API names unchanged.
		""";

	private static final String LOW_DEPTH_PROMPT = """
		Answer depth: LOW (fast). Keep it short: one-sentence summary, then up to 3 short bullet points.
		Keep the entire answer within 300 Korean characters.
		""";

	private static final String DEFAULT_DEPTH_PROMPT = """
		Answer depth: DEFAULT (standard). Start with a one-sentence summary, followed by 3 to 5 short
		bullet points. Keep the entire answer within 600 Korean characters unless the user explicitly
		asks for detail.
		""";

	private static final String HIGH_DEPTH_PROMPT = """
		Answer depth: HIGH (deep reasoning). Provide a thorough, multi-angle analysis - consider
		implementation details, edge cases, and trade-offs where relevant. Use a short summary
		followed by up to 8 detailed bullet points, grouped by concern if that helps. You may exceed
		600 Korean characters when the question genuinely warrants it.
		""";

	private final OllamaChatModel oracleRagChatModel;
	private final ProjectRagRetriever projectRagRetriever;

	public AiChatService(
		@Qualifier("oracleRagChatModel") OllamaChatModel oracleRagChatModel,
		@Lazy ProjectRagRetriever projectRagRetriever
	) {
		this.oracleRagChatModel = oracleRagChatModel;
		this.projectRagRetriever = projectRagRetriever;
	}

	public ChatResponse chat(Long projectId, String question) {
		return chat(projectId, question, ThinkingLevel.DEFAULT);
	}

	public ChatResponse chat(Long projectId, String question, ThinkingLevel level) {
		if (projectId == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "projectId is required.");
		}
		if (!StringUtils.hasText(question)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "question is required.");
		}

		ThinkingLevel effectiveLevel = level == null ? ThinkingLevel.DEFAULT : level;
		List<String> contexts = projectRagRetriever.retrieve(projectId, question.trim(), effectiveLevel);

		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from(buildSystemPrompt(effectiveLevel)));
		messages.add(UserMessage.from(buildUserPrompt(projectId, question.trim(), contexts)));

		String answer = oracleRagChatModel.chat(messages).aiMessage().text();
		if (!StringUtils.hasText(answer)) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "The RAG chat model returned an empty response.");
		}

		return new ChatResponse(answer.trim(), contexts);
	}

	private String buildSystemPrompt(ThinkingLevel level) {
		String depthPrompt = switch (level) {
			case LOW -> LOW_DEPTH_PROMPT;
			case HIGH -> HIGH_DEPTH_PROMPT;
			default -> DEFAULT_DEPTH_PROMPT;
		};
		return BASE_SYSTEM_PROMPT + "\n" + depthPrompt;
	}

	private String buildUserPrompt(Long projectId, String question, List<String> contexts) {
		if (contexts.isEmpty()) {
			return """
				Project ID: %d

				Project-isolated official document context: (none found for this question)

				User question:
				%s

				Instructions:
				- No project-specific document context was found for this question.
				- Answer from your own general knowledge instead of refusing.
				- Briefly note (one short sentence) that project-specific documentation was limited.
				- Do not invent specific project APIs, tables, or architecture details.
				""".formatted(projectId, question);
		}

		String joinedContext = String.join("\n\n---\n\n", contexts);
		return """
			Project ID: %d

			Project-isolated official document context:
			%s

			User question:
			%s

			Instructions:
			- Use only documents whose metadata projectId matches the Project ID above.
			- If the context does not fully answer the question, fill the gap with your own general
			  knowledge rather than refusing, and briefly note that project documentation was partial.
			- Do not invent specific project APIs, tables, or architecture details.
			- Do not reproduce raw document context or source code verbatim.
			""".formatted(projectId, joinedContext, question);
	}
}
