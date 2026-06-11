package com.weai.server.global.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import com.weai.server.domain.ai.debate.agent.BackendAi;
import com.weai.server.domain.ai.debate.agent.FrontendAi;
import com.weai.server.domain.ai.debate.agent.InspectorAi;
import com.weai.server.domain.ai.debate.agent.OracleAi;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

	@Bean("oracleEmbeddingModel")
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

}
