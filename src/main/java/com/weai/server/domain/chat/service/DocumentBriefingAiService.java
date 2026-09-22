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
 * Generates a real, LLM-backed document briefing (summary / key points / action items / risks /
 * keywords). Replaces the earlier regex-and-keyword-matching heuristic in
 * {@link ChatDocumentMeetingService}, which never called an AI model despite the "AI 브리핑"
 * product framing.
 */
@Slf4j
@Service
public class DocumentBriefingAiService {

	private static final int MAX_INPUT_LENGTH = 20_000;

	private static final String SYSTEM_PROMPT = """
		You are SYNAIPSE's document briefing assistant.
		Read the supplied document text and produce a concise, faithful briefing for a project team.
		Respond in the same language the document is written in.
		Return exactly one JSON object with this exact schema and nothing else (no markdown, no code fences):
		{
		  "summary": "2-4 sentence overview of the document",
		  "key_points": ["...", "..."],
		  "action_items": ["...", "..."],
		  "risks": ["...", "..."],
		  "keywords": ["...", "..."]
		}
		Rules:
		- "summary" must be a single non-empty string, at most 500 characters.
		- "key_points", "action_items", "risks", and "keywords" must be JSON arrays of short strings; use an empty array if none apply.
		- Only list an action item if the document implies a concrete follow-up task.
		- Only list a risk if the document mentions or clearly implies an actual concern, limitation, or blocker.
		- Do not invent information that is not present in the document.
		""";

	private final ObjectMapper objectMapper;
	private final OllamaChatModel briefingModel;

	public DocumentBriefingAiService(@Qualifier("documentBriefingChatModel") OllamaChatModel briefingModel) {
		this.objectMapper = new ObjectMapper();
		this.briefingModel = briefingModel;
	}

	public BriefingDraft generate(String documentText) {
		if (!StringUtils.hasText(documentText)) {
			throw new ApiException(ErrorCode.DOCUMENT_TEXT_NOT_EXTRACTED);
		}

		List<ChatMessage> messages = List.of(
			SystemMessage.from(SYSTEM_PROMPT),
			UserMessage.from(buildPrompt(truncate(documentText.trim(), MAX_INPUT_LENGTH)))
		);

		String rawJson = briefingModel.chat(messages).aiMessage().text();
		if (!StringUtils.hasText(rawJson)) {
			throw new ApiException(ErrorCode.DOCUMENT_BRIEFING_CREATE_FAILED, "The AI briefing model returned an empty response.");
		}

		try {
			JsonNode root = objectMapper.readTree(rawJson);
			String summary = readRequiredText(root, "summary");
			return new BriefingDraft(
				summary,
				readStringArray(root, "key_points"),
				readStringArray(root, "action_items"),
				readStringArray(root, "risks"),
				readStringArray(root, "keywords")
			);
		} catch (Exception exception) {
			log.error("Failed to parse the AI briefing response as JSON", exception);
			throw new ApiException(ErrorCode.DOCUMENT_BRIEFING_CREATE_FAILED, "Failed to parse the AI briefing response as JSON.", exception);
		}
	}

	private String buildPrompt(String documentText) {
		return """
			Document text:
			%s
			""".formatted(documentText);
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

	public record BriefingDraft(
		String summary,
		List<String> keyPoints,
		List<String> actionItems,
		List<String> risks,
		List<String> keywords
	) {
	}
}
