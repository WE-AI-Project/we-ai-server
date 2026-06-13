package com.weai.server.domain.ai.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weai.server.domain.ai.rag.ProjectRagContext;
import com.weai.server.domain.ai.rag.ProjectRagContextService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiQaService {

	private static final String SYSTEM_PROMPT = """
		너는 코드 QA 전문가 'Sync'야.
		너의 역할은 diff를 읽고 실제 버그 가능성, 개선 포인트, 그리고 semantic commit 메시지를 제안하는 것이다.
		반드시 순수 JSON 객체 하나만 반환해.
		마크다운, 코드펜스, 설명 문장, 백틱은 절대 포함하지 마.
		반환 JSON 스키마는 정확히 아래 키만 사용해:
		{
		  "bug_report": "...",
		  "optimization": "...",
		  "commit_msg": "..."
		}
		각 필드는 비어 있지 않은 문자열이어야 한다.
		commit_msg는 conventional commits 스타일을 따르고, 소문자 타입으로 시작해야 한다.
		""";

	private static final String FEW_SHOT_EXAMPLE_ONE = """
		Example 1 input diff:
		diff --git a/src/main/java/com/example/UserService.java b/src/main/java/com/example/UserService.java
		@@
		- return userRepository.findById(id).get();
		+ return userRepository.findById(id).orElse(null);

		Example 1 ideal JSON:
		{"bug_report":"Returning null avoids the immediate exception but can introduce a later NullPointerException in callers that expect a user to exist.","optimization":"Throw a domain-specific not-found exception or return Optional consistently so the null contract does not become ambiguous.","commit_msg":"fix: replace nullable user lookup with explicit not-found handling"}
		""";

	private static final String FEW_SHOT_EXAMPLE_TWO = """
		Example 2 input diff:
		diff --git a/src/main/java/com/example/OrderController.java b/src/main/java/com/example/OrderController.java
		@@
		- @GetMapping("/orders")
		+ @PostMapping("/orders")
		- public List<OrderResponse> getOrders() {
		+ public List<OrderResponse> getOrders() {

		Example 2 ideal JSON:
		{"bug_report":"Changing the endpoint from GET to POST can break existing clients and violates the read-only semantics of the endpoint without any matching request-body change.","optimization":"Keep the retrieval endpoint as GET unless there is a clear write-side requirement, and document the change if the API contract must evolve.","commit_msg":"fix: restore get mapping for order retrieval endpoint"}
		""";

	private final OllamaChatModel jsonQaModel;
	private final ObjectMapper objectMapper;
	private final ProjectRagContextService projectRagContextService;

	public AiQaService(
		ProjectRagContextService projectRagContextService,
		@Value("${ai.qa.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.qa.model-name:${AI_QA_MODEL_NAME:qwen2.5-coder}}") String modelName,
		@Value("${ai.qa.timeout:${AI_QA_TIMEOUT:PT60S}}") Duration timeout
	) {
		this.objectMapper = new ObjectMapper();
		this.projectRagContextService = projectRagContextService;
		this.jsonQaModel = OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.1)
			.timeout(timeout)
			.format("json")
			.build();
	}

	public QaResponse analyze(Long projectId, String diff) {
		if (projectId == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "projectId is required.");
		}
		if (!StringUtils.hasText(diff)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "diff is required.");
		}

		ProjectRagContext ragContext = projectRagContextService.retrieve(projectId, buildRagQuery(diff.trim()));
		if (ragContext.isEmpty()) {
			throw new ApiException(
				ErrorCode.INVALID_INPUT,
				"No project RAG context was found for this QA request. Index project documents before running AI QA."
			);
		}

		List<ChatMessage> messages = new ArrayList<>();
		messages.add(SystemMessage.from(SYSTEM_PROMPT));
		messages.add(UserMessage.from(FEW_SHOT_EXAMPLE_ONE));
		messages.add(AiMessage.from("""
			{"bug_report":"Returning null avoids the immediate exception but can introduce a later NullPointerException in callers that expect a user to exist.","optimization":"Throw a domain-specific not-found exception or return Optional consistently so the null contract does not become ambiguous.","commit_msg":"fix: replace nullable user lookup with explicit not-found handling"}
			"""));
		messages.add(UserMessage.from(FEW_SHOT_EXAMPLE_TWO));
		messages.add(AiMessage.from("""
			{"bug_report":"Changing the endpoint from GET to POST can break existing clients and violates the read-only semantics of the endpoint without any matching request-body change.","optimization":"Keep the retrieval endpoint as GET unless there is a clear write-side requirement, and document the change if the API contract must evolve.","commit_msg":"fix: restore get mapping for order retrieval endpoint"}
			"""));
		messages.add(UserMessage.from(buildAnalysisPrompt(projectId, diff.trim(), ragContext.formatted())));

		String rawJson = jsonQaModel.generate(messages).content().text();
		if (!StringUtils.hasText(rawJson)) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "The AI QA model returned an empty response.");
		}

		try {
			JsonNode root = objectMapper.readTree(rawJson);
			String bugReport = readRequiredText(root, "bug_report");
			String optimization = readRequiredText(root, "optimization");
			String commitMsg = readRequiredText(root, "commit_msg");
			return new QaResponse(bugReport, optimization, commitMsg);
		} catch (Exception exception) {
			throw new ApiException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"Failed to parse the AI QA response as JSON."
			);
		}
	}

	private String buildRagQuery(String diff) {
		return """
			Code QA diff analysis request.

			Diff:
			%s
			""".formatted(diff);
	}

	private String buildAnalysisPrompt(Long projectId, String diff, String ragContext) {
		return """
			Project ID: %d

			Project-isolated official document context:
			%s

			Analyze the following git diff.
			Return exactly one JSON object with bug_report, optimization, and commit_msg.
			Focus on realistic bug risk, code quality, and a concise semantic commit message.
			Use the project context above as the authority for architecture, conventions, APIs, and naming.
			If the diff conflicts with the project context, call that out in bug_report or optimization.

			Diff:
			%s
			""".formatted(projectId, ragContext, diff);
	}

	private String readRequiredText(JsonNode root, String fieldName) {
		JsonNode node = root.get(fieldName);
		if (node == null || !node.isTextual() || !StringUtils.hasText(node.asText())) {
			throw new IllegalArgumentException("Missing required field: " + fieldName);
		}
		return node.asText().trim();
	}
}
