package com.weai.server.domain.ai.chat;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiChatService {

	private static final String SYSTEM_PROMPT = """
		너는 SYNAIPSE 프로젝트의 기술 지식 비서 'Oracle'이야.
		제공된 공식 문서 컨텍스트를 기반으로만 답변하고, 모르면 모른다고 말해.
		추측하거나 일반 상식으로 빈칸을 채우지 마.
		답변은 간결하지만 실무적으로 도움이 되게 작성해.
		""";

	private static final String NO_CONTEXT_MESSAGE = "모르겠습니다. 제공된 공식 문서 컨텍스트에서 관련 내용을 찾지 못했습니다.";

	private final OllamaChatModel oracleRagChatModel;
	private final ContentRetriever oracleContentRetriever;

	public AiChatService(
		@Qualifier("oracleRagChatModel") OllamaChatModel oracleRagChatModel,
		@Lazy @Qualifier("oracleContentRetriever") ContentRetriever oracleContentRetriever
	) {
		this.oracleRagChatModel = oracleRagChatModel;
		this.oracleContentRetriever = oracleContentRetriever;
	}

	public ChatResponse chat(String question) {
		if (!StringUtils.hasText(question)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "question is required.");
		}

		List<Content> retrievedContents = oracleContentRetriever.retrieve(Query.from(question.trim()));
		List<String> contexts = retrievedContents.stream()
			.map(content -> content.textSegment().text())
			.filter(StringUtils::hasText)
			.toList();

		if (contexts.isEmpty()) {
			return new ChatResponse(NO_CONTEXT_MESSAGE, List.of());
		}

		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from(SYSTEM_PROMPT));
		messages.add(UserMessage.from(buildUserPrompt(question.trim(), contexts)));

		String answer = oracleRagChatModel.generate(messages).content().text();
		if (!StringUtils.hasText(answer)) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "The RAG chat model returned an empty response.");
		}

		return new ChatResponse(answer.trim(), contexts);
	}

	private String buildUserPrompt(String question, List<String> contexts) {
		String joinedContext = String.join("\n\n---\n\n", contexts);
		return """
			Official document context:
			%s

			User question:
			%s

			Instructions:
			- Answer only from the context above.
			- If the context does not contain enough information, say you do not know.
			- Do not invent APIs, tables, or architecture details.
			""".formatted(joinedContext, question);
	}
}
