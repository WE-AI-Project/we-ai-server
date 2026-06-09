package com.weai.server.domain.ai.debate;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiDebateService {

	private static final String ORACLE_SYSTEM_PROMPT = """
		You are Oracle, the PM agent.
		Your job is to define the situation crisply, surface missing assumptions,
		and later produce the final recommendation after reviewing the whole debate.
		Be structured, concise, and practical.
		""";

	private static final String ARCHITECT_SYSTEM_PROMPT = """
		You are Architect, the backend architecture agent.
		Focus on API design, DB schema, transaction boundaries, scalability,
		and maintainability. Point out technical trade-offs clearly.
		""";

	private static final String SYNC_SYSTEM_PROMPT = """
		You are Sync, the QA and reviewer agent.
		Focus on bug risk, edge cases, validation gaps, test strategy,
		and whether the plan actually matches product intent.
		""";

	private final OllamaChatModel ollamaChatModel;
	private final int maxMemoryMessages;

	public AiDebateService(
		@Qualifier("debateOllamaChatModel")
		OllamaChatModel ollamaChatModel,
		@Value("${ai.debate.max-memory-messages:12}") int maxMemoryMessages
	) {
		this.ollamaChatModel = ollamaChatModel;
		this.maxMemoryMessages = maxMemoryMessages;
	}

	public DebateResponse debate(String query) {
		if (!StringUtils.hasText(query)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "query is required.");
		}

		MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(maxMemoryMessages);
		List<DebateResponse.DebateTurn> debateHistory = new ArrayList<>();

		memory.add(UserMessage.from("Original user question:\n" + query.trim()));

		String oracleOpening = runAgent(
			memory,
			"Oracle",
			"PM",
			ORACLE_SYSTEM_PROMPT,
			"""
			First turn.
			Reframe the user's problem, define the situation, list the key constraints,
			and tell Architect and Sync what they should pay attention to.
			Keep it focused on moving the debate forward.
			""",
			debateHistory
		);

		String architectOpinion = runAgent(
			memory,
			"Architect",
			"Backend",
			ARCHITECT_SYSTEM_PROMPT,
			"""
			Review the conversation so far and provide a backend-oriented analysis.
			Focus on API boundaries, persistence model, data flow, operational concerns,
			and implementation trade-offs.
			""",
			debateHistory
		);

		String syncOpinion = runAgent(
			memory,
			"Sync",
			"QA/Reviewer",
			SYNC_SYSTEM_PROMPT,
			"""
			Review the whole conversation so far.
			Challenge weak assumptions, identify bugs and QA risks,
			and comment on whether the plan still fits the product goal.
			""",
			debateHistory
		);

		String finalConclusion = runAgent(
			memory,
			"Oracle",
			"PM",
			ORACLE_SYSTEM_PROMPT,
			"""
			Final turn.
			Review the full conversation history and produce the final conclusion.
			Summarize the recommended direction, the main technical trade-off,
			and the next implementation steps in a compact, decision-ready format.
			""",
			debateHistory
		);

		return new DebateResponse(
			query.trim(),
			oracleOpening,
			architectOpinion,
			syncOpinion,
			finalConclusion,
			List.copyOf(debateHistory)
		);
	}

	private String runAgent(
		MessageWindowChatMemory memory,
		String agent,
		String role,
		String systemPrompt,
		String instruction,
		List<DebateResponse.DebateTurn> debateHistory
	) {
		memory.add(UserMessage.from(agent + " instruction:\n" + instruction));

		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from(systemPrompt));
		messages.addAll(memory.messages());

		String response = ollamaChatModel.generate(messages).content().text();
		if (!StringUtils.hasText(response)) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "The AI debate agent returned an empty response.");
		}

		memory.add(AiMessage.from(agent + " response:\n" + response));
		debateHistory.add(new DebateResponse.DebateTurn(agent, role, response));
		return response;
	}
}
