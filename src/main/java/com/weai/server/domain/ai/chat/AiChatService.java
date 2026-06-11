package com.weai.server.domain.ai.chat;

import com.weai.server.domain.ai.rag.ProjectRagRetriever;
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

	private static final String SYSTEM_PROMPT = """
		You are Oracle, SYNAIPSE's internal project knowledge assistant.
		Answer only from the supplied project-isolated official document context.
		If the context is insufficient, say that you do not know.
		Do not invent APIs, tables, or architecture details.
		Write in Korean, but keep code identifiers and API names unchanged.
		""";

	private static final String NO_CONTEXT_MESSAGE =
		"모르겠습니다. 해당 프로젝트의 공식 문서 컨텍스트에서 관련 내용을 찾지 못했습니다.";

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
		if (projectId == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "projectId is required.");
		}
		if (!StringUtils.hasText(question)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "question is required.");
		}

		List<String> contexts = projectRagRetriever.retrieve(projectId, question.trim());
		if (contexts.isEmpty()) {
			return new ChatResponse(NO_CONTEXT_MESSAGE, List.of());
		}

		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from(SYSTEM_PROMPT));
		messages.add(UserMessage.from(buildUserPrompt(projectId, question.trim(), contexts)));

		String answer = oracleRagChatModel.generate(messages).content().text();
		if (!StringUtils.hasText(answer)) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "The RAG chat model returned an empty response.");
		}

		return new ChatResponse(answer.trim(), contexts);
	}

	private String buildUserPrompt(Long projectId, String question, List<String> contexts) {
		String joinedContext = String.join("\n\n---\n\n", contexts);
		return """
			Project ID: %d

			Project-isolated official document context:
			%s

			User question:
			%s

			Instructions:
			- Use only documents whose metadata projectId matches the Project ID above.
			- If the context does not contain enough information, say you do not know.
			- Do not invent APIs, tables, or architecture details.
			""".formatted(projectId, joinedContext, question);
	}
}
