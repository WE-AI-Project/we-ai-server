package com.weai.server.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Generates a real, LLM-backed meeting summary and action-item list from a meeting's raw chat
 * transcript. Replaces the previous behavior where "AI 요약" was actually just the client
 * concatenating participant names and raw message lines with no model call at all
 * (see {@code ChatDocumentMeetingService#resolveSummary}, which only truncated whatever text
 * it was given), the same class of fake-AI issue {@link DocumentBriefingAiService} fixed for
 * document briefings.
 */
@Slf4j
@Service
public class MeetingSummaryAiService {

	private static final int MAX_INPUT_LENGTH = 20_000;

	private static final String SYSTEM_PROMPT = """
		You are SYNAIPSE's meeting summary assistant.
		Read the supplied chat transcript from a project team meeting and produce a concise,
		faithful summary plus any concrete follow-up action items.
		Respond in the same language the transcript is written in.
		Return exactly one JSON object with this exact schema and nothing else (no markdown, no code fences):
		{
		  "summary": "2-5 sentence overview of what was discussed and decided",
		  "action_items": ["...", "..."]
		}
		Rules:
		- "summary" must be a single non-empty string, at most 500 characters.
		- "action_items" must be a JSON array of short strings; use an empty array if the transcript
		  implies no concrete follow-up task.
		- Do not invent information, decisions, or tasks that are not present in the transcript.
		""";

	private final ObjectMapper objectMapper;
	private final OllamaChatModel meetingSummaryModel;

	public MeetingSummaryAiService(@Qualifier("meetingSummaryChatModel") OllamaChatModel meetingSummaryModel) {
		this.objectMapper = new ObjectMapper();
		this.meetingSummaryModel = meetingSummaryModel;
	}

	public MeetingSummaryDraft generate(String transcript) {
		if (!StringUtils.hasText(transcript)) {
			throw new ApiException(ErrorCode.MEETING_MINUTE_CONTENT_REQUIRED);
		}

		List<ChatMessage> messages = List.of(
			SystemMessage.from(SYSTEM_PROMPT),
			UserMessage.from(buildPrompt(truncate(transcript.trim(), MAX_INPUT_LENGTH)))
		);

		String rawJson = meetingSummaryModel.chat(messages).aiMessage().text();
		if (!StringUtils.hasText(rawJson)) {
			throw new ApiException(ErrorCode.MEETING_SUMMARY_CREATE_FAILED, "The AI meeting summary model returned an empty response.");
		}

		try {
			JsonNode root = objectMapper.readTree(rawJson);
			String summary = readRequiredText(root, "summary");
			return new MeetingSummaryDraft(summary, readStringArray(root, "action_items"));
		} catch (Exception exception) {
			log.error("Failed to parse the AI meeting summary response as JSON", exception);
			throw new ApiException(ErrorCode.MEETING_SUMMARY_CREATE_FAILED, "Failed to parse the AI meeting summary response as JSON.", exception);
		}
	}

	private String buildPrompt(String transcript) {
		return """
			Meeting transcript:
			%s
			""".formatted(transcript);
	}

	private String truncate(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private String readRequiredText(JsonNode root, String fieldName) {
		JsonNode node = root.get(fieldName);
		if (node == null || !node.isTextual() || !StringUtils.hasText(node.asText())) {
			throw new IllegalArgumentException("Missing required field: " + fieldName);
		}
		return node.asText().trim();
	}

	private List<String> readStringArray(JsonNode root, String fieldName) {
		JsonNode node = root.get(fieldName);
		if (node == null || !node.isArray()) {
			return List.of();
		}
		List<String> values = new ArrayList<>();
		node.forEach(item -> {
			if (item.isTextual() && StringUtils.hasText(item.asText())) {
				values.add(item.asText().trim());
			}
		});
		return List.copyOf(values);
	}

	public record MeetingSummaryDraft(String summary, List<String> actionItems) {
	}
}
