package com.weai.server.global.config;

import com.weai.server.domain.ai.debate.agent.BackendAi;
import com.weai.server.domain.ai.debate.agent.FrontendAi;
import com.weai.server.domain.ai.debate.agent.InspectorAi;
import com.weai.server.domain.ai.debate.agent.OracleAi;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AiConfig {

	/**
	 * Bounds concurrent AI debate SSE streams instead of the previous {@code new Thread()} per
	 * request, which had no ceiling and could exhaust OS threads under concurrent load. Rejected
	 * tasks (queue full) throw {@link org.springframework.core.task.TaskRejectedException} back to
	 * the caller rather than silently spawning yet another unbounded thread.
	 */
	@Bean("aiDebateStreamExecutor")
	public Executor aiDebateStreamExecutor(
		@Value("${ai.debate.stream.core-pool-size:4}") int corePoolSize,
		@Value("${ai.debate.stream.max-pool-size:16}") int maxPoolSize,
		@Value("${ai.debate.stream.queue-capacity:32}") int queueCapacity
	) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("ai-debate-stream-");
		executor.setDaemon(true);
		executor.initialize();
		return executor;
	}

	private static final String CF_ACCESS_CLIENT_ID_HEADER = "CF-Access-Client-Id";
	private static final String CF_ACCESS_CLIENT_SECRET_HEADER = "CF-Access-Client-Secret";

	@Bean("ollamaCustomHeaders")
	public Map<String, String> ollamaCustomHeaders(
		@Value("${OLLAMA_ACCESS_CLIENT_ID:}") String clientId,
		@Value("${OLLAMA_ACCESS_CLIENT_SECRET:}") String clientSecret
	) {
		if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
			if (StringUtils.hasText(clientId) || StringUtils.hasText(clientSecret)) {
				// 하나만 설정된 경우 이전에는 아무 경고 없이 그냥 인증 헤더 없는 요청으로 조용히
				// 넘어갔다 - 운영 환경에서 오타/설정 누락을 알아챌 방법이 없었다.
				log.warn(
					"Only one of OLLAMA_ACCESS_CLIENT_ID/OLLAMA_ACCESS_CLIENT_SECRET is set. "
						+ "Cloudflare Access headers will NOT be sent, so Ollama calls will go out unauthenticated. "
						+ "Set both or neither."
				);
			}
			return Collections.emptyMap();
		}

		return Map.of(
			CF_ACCESS_CLIENT_ID_HEADER, clientId,
			CF_ACCESS_CLIENT_SECRET_HEADER, clientSecret
		);
	}

	@Bean("debateQwenChatModel")
	public OllamaChatModel debateQwenChatModel(
		@Value("${ai.debate.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.debate.qwen-model-name:${AI_DEBATE_QWEN_MODEL_NAME:qwen2.5-coder}}") String modelName,
		@Value("${ai.debate.timeout:${AI_DEBATE_TIMEOUT:PT60S}}") Duration timeout,
		@org.springframework.beans.factory.annotation.Qualifier("ollamaCustomHeaders") Map<String, String> customHeaders
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.2)
			.timeout(timeout)
			.customHeaders(customHeaders)
			.build();
	}

	@Bean("debateLlamaChatModel")
	public OllamaChatModel debateLlamaChatModel(
		@Value("${ai.debate.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.debate.llama-model-name:${AI_DEBATE_LLAMA_MODEL_NAME:llama3.1}}") String modelName,
		@Value("${ai.debate.timeout:${AI_DEBATE_TIMEOUT:PT60S}}") Duration timeout,
		@org.springframework.beans.factory.annotation.Qualifier("ollamaCustomHeaders") Map<String, String> customHeaders
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.2)
			.timeout(timeout)
			.customHeaders(customHeaders)
			.build();
	}

	@Bean
	public OracleAi oracleAi(@org.springframework.beans.factory.annotation.Qualifier("debateLlamaChatModel") OllamaChatModel chatModel) {
		return AiServices.create(OracleAi.class, chatModel);
	}

	@Bean
	public BackendAi backendAi(@org.springframework.beans.factory.annotation.Qualifier("debateQwenChatModel") OllamaChatModel chatModel) {
		return AiServices.create(BackendAi.class, chatModel);
	}

	@Bean
	public FrontendAi frontendAi(@org.springframework.beans.factory.annotation.Qualifier("debateLlamaChatModel") OllamaChatModel chatModel) {
		return AiServices.create(FrontendAi.class, chatModel);
	}

	@Bean
	public InspectorAi inspectorAi(@org.springframework.beans.factory.annotation.Qualifier("debateQwenChatModel") OllamaChatModel chatModel) {
		return AiServices.create(InspectorAi.class, chatModel);
	}

	@Bean("oracleRagChatModel")
	@Lazy
	public OllamaChatModel oracleRagChatModel(
		@Value("${ai.chat.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.chat.model-name:${AI_CHAT_MODEL_NAME:llama3.1}}") String modelName,
		@Value("${ai.chat.timeout:${AI_CHAT_TIMEOUT:PT60S}}") Duration timeout,
		@org.springframework.beans.factory.annotation.Qualifier("ollamaCustomHeaders") Map<String, String> customHeaders
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.1)
			.timeout(timeout)
			.customHeaders(customHeaders)
			.build();
	}

	@Bean("qaChatModel")
	public OllamaChatModel qaChatModel(
		@Value("${ai.qa.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.qa.model-name:${AI_QA_MODEL_NAME:qwen2.5-coder}}") String modelName,
		@Value("${ai.qa.timeout:${AI_QA_TIMEOUT:PT60S}}") Duration timeout,
		@org.springframework.beans.factory.annotation.Qualifier("ollamaCustomHeaders") Map<String, String> customHeaders
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.1)
			.timeout(timeout)
			.responseFormat(ResponseFormat.JSON)
			.customHeaders(customHeaders)
			.build();
	}

	@Bean("commitJsonChatModel")
	public OllamaChatModel commitJsonChatModel(
		@Value("${ai.commit.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.commit.model-name:${AI_COMMIT_MODEL_NAME:qwen2.5-coder}}") String modelName,
		@Value("${ai.commit.timeout:${AI_COMMIT_TIMEOUT:PT60S}}") Duration timeout,
		@org.springframework.beans.factory.annotation.Qualifier("ollamaCustomHeaders") Map<String, String> customHeaders
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.1)
			.timeout(timeout)
			.responseFormat(ResponseFormat.JSON)
			.customHeaders(customHeaders)
			.build();
	}

	@Bean("documentBriefingChatModel")
	public OllamaChatModel documentBriefingChatModel(
		@Value("${ai.briefing.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.briefing.model-name:${AI_BRIEFING_MODEL_NAME:llama3.1}}") String modelName,
		@Value("${ai.briefing.timeout:${AI_BRIEFING_TIMEOUT:PT60S}}") Duration timeout,
		@org.springframework.beans.factory.annotation.Qualifier("ollamaCustomHeaders") Map<String, String> customHeaders
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.1)
			.timeout(timeout)
			.responseFormat(ResponseFormat.JSON)
			.customHeaders(customHeaders)
			.build();
	}

	@Bean("meetingSummaryChatModel")
	public OllamaChatModel meetingSummaryChatModel(
		@Value("${ai.meeting.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.meeting.model-name:${AI_MEETING_MODEL_NAME:llama3.1}}") String modelName,
		@Value("${ai.meeting.timeout:${AI_MEETING_TIMEOUT:PT60S}}") Duration timeout,
		@org.springframework.beans.factory.annotation.Qualifier("ollamaCustomHeaders") Map<String, String> customHeaders
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.1)
			.timeout(timeout)
			.responseFormat(ResponseFormat.JSON)
			.customHeaders(customHeaders)
			.build();
	}

	@Bean("oracleEmbeddingModel")
	@Lazy
	public EmbeddingModel oracleEmbeddingModel(
		@Value("${ai.chat.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.chat.embedding-model-name:${AI_CHAT_EMBEDDING_MODEL_NAME:nomic-embed-text}}") String modelName,
		@Value("${ai.chat.timeout:${AI_CHAT_TIMEOUT:PT60S}}") Duration timeout,
		@org.springframework.beans.factory.annotation.Qualifier("ollamaCustomHeaders") Map<String, String> customHeaders
	) {
		return OllamaEmbeddingModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.timeout(timeout)
			.customHeaders(customHeaders)
			.build();
	}

	@Bean("oracleChromaEmbeddingStore")
	@ConditionalOnProperty(name = "ai.chat.chroma.enabled", havingValue = "true", matchIfMissing = true)
	@Lazy
	public EmbeddingStore<TextSegment> oracleChromaEmbeddingStore(
		@Value("${ai.chat.chroma.base-url:${CHROMA_BASE_URL:http://localhost:8000}}") String baseUrl,
		@Value("${ai.chat.chroma.collection-name:${CHROMA_COLLECTION_NAME:synaipse-docs}}") String collectionName,
		@Value("${ai.chat.chroma.tenant-name:${CHROMA_TENANT_NAME:default_tenant}}") String tenantName,
		@Value("${ai.chat.chroma.database-name:${CHROMA_DATABASE_NAME:default_database}}") String databaseName,
		@Value("${ai.chat.chroma.timeout:${CHROMA_TIMEOUT:PT10S}}") Duration timeout
	) {
		return ChromaEmbeddingStore.builder()
			.apiVersion(ChromaApiVersion.V2)
			.baseUrl(baseUrl)
			.tenantName(tenantName)
			.databaseName(databaseName)
			.collectionName(collectionName)
			.timeout(timeout)
			.build();
	}

	@Bean("oracleChromaEmbeddingStore")
	@ConditionalOnProperty(name = "ai.chat.chroma.enabled", havingValue = "false")
	public EmbeddingStore<TextSegment> testOracleEmbeddingStore() {
		return new InMemoryEmbeddingStore<>();
	}

}
