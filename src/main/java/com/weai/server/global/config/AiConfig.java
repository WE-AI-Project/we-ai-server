package com.weai.server.global.config;

import com.weai.server.domain.ai.debate.agent.BackendAi;
import com.weai.server.domain.ai.debate.agent.FrontendAi;
import com.weai.server.domain.ai.debate.agent.InspectorAi;
import com.weai.server.domain.ai.debate.agent.OracleAi;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class AiConfig {

	@Bean("debateQwenChatModel")
	public OllamaChatModel debateQwenChatModel(
		@Value("${ai.debate.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.debate.qwen-model-name:${AI_DEBATE_QWEN_MODEL_NAME:qwen2.5-coder}}") String modelName,
		@Value("${ai.debate.timeout:${AI_DEBATE_TIMEOUT:PT60S}}") Duration timeout
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.2)
			.timeout(timeout)
			.build();
	}

	@Bean("debateLlamaChatModel")
	public OllamaChatModel debateLlamaChatModel(
		@Value("${ai.debate.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.debate.llama-model-name:${AI_DEBATE_LLAMA_MODEL_NAME:llama3.1}}") String modelName,
		@Value("${ai.debate.timeout:${AI_DEBATE_TIMEOUT:PT60S}}") Duration timeout
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.2)
			.timeout(timeout)
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
		@Value("${ai.chat.timeout:${AI_CHAT_TIMEOUT:PT60S}}") Duration timeout
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.1)
			.timeout(timeout)
			.build();
	}

	@Bean("commitJsonChatModel")
	public OllamaChatModel commitJsonChatModel(
		@Value("${ai.commit.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.commit.model-name:${AI_COMMIT_MODEL_NAME:qwen2.5-coder}}") String modelName,
		@Value("${ai.commit.timeout:${AI_COMMIT_TIMEOUT:PT60S}}") Duration timeout
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.1)
			.timeout(timeout)
			.format("json")
			.build();
	}

	@Bean("oracleEmbeddingModel")
	@Lazy
	public EmbeddingModel oracleEmbeddingModel(
		@Value("${ai.chat.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.yhy-server.com}}") String baseUrl,
		@Value("${ai.chat.embedding-model-name:${AI_CHAT_EMBEDDING_MODEL_NAME:nomic-embed-text}}") String modelName,
		@Value("${ai.chat.timeout:${AI_CHAT_TIMEOUT:PT60S}}") Duration timeout
	) {
		return OllamaEmbeddingModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.timeout(timeout)
			.build();
	}

	@Bean("oracleChromaEmbeddingStore")
	@ConditionalOnProperty(name = "ai.chat.chroma.enabled", havingValue = "true", matchIfMissing = true)
	@Lazy
	public EmbeddingStore<TextSegment> oracleChromaEmbeddingStore(
		@Value("${ai.chat.chroma.base-url:${CHROMA_BASE_URL:http://localhost:8000}}") String baseUrl,
		@Value("${ai.chat.chroma.collection-name:${CHROMA_COLLECTION_NAME:synaipse-docs}}") String collectionName,
		@Value("${ai.chat.chroma.timeout:${CHROMA_TIMEOUT:PT10S}}") Duration timeout
	) {
		return ChromaEmbeddingStore.builder()
			.baseUrl(baseUrl)
			.collectionName(collectionName)
			.timeout(timeout)
			.build();
	}

	@Bean("oracleChromaEmbeddingStore")
	@ConditionalOnProperty(name = "ai.chat.chroma.enabled", havingValue = "false")
	public EmbeddingStore<TextSegment> testOracleEmbeddingStore() {
		return new EmptySearchEmbeddingStore<>();
	}

	private static final class EmptySearchEmbeddingStore<Embedded> implements EmbeddingStore<Embedded> {

		@Override
		public String add(Embedding embedding) {
			return UUID.randomUUID().toString();
		}

		@Override
		public void add(String id, Embedding embedding) {
		}

		@Override
		public String add(Embedding embedding, Embedded embedded) {
			return UUID.randomUUID().toString();
		}

		@Override
		public List<String> addAll(List<Embedding> embeddings) {
			List<String> ids = new ArrayList<>();
			for (int i = 0; i < embeddings.size(); i++) {
				ids.add(UUID.randomUUID().toString());
			}
			return ids;
		}

		@Override
		public List<String> addAll(List<Embedding> embeddings, List<Embedded> embedded) {
			return addAll(embeddings);
		}

		@Override
		public EmbeddingSearchResult<Embedded> search(EmbeddingSearchRequest request) {
			return new EmbeddingSearchResult<>(List.of());
		}
	}

}
