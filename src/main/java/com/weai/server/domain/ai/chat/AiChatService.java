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
		Keep the answer concise and presentation-friendly.
		Start with a one-sentence summary, followed by 3 to 5 short bullet points.
		Do not quote raw context, source code, or repeat the same explanation.
		Keep the entire answer within 600 Korean characters unless the user explicitly asks for detail.
		""";

	private static final String NO_CONTEXT_MESSAGE =
		"주의: 충분한 프로젝트의 표본이 없습니다. 프로젝트 문서를 추가하면 더 정확한 답변을 받을 수 있습니다.";

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

		String answer = oracleRagChatModel.chat(messages).aiMessage().text();
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
			- Return one short summary followed by 3 to 5 concise bullet points.
			- Do not reproduce raw document context or source code.
			- Keep the answer within 600 Korean characters.
			""".formatted(projectId, joinedContext, question);
	}
}
