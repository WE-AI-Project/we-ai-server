package com.weai.server.global.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

	@Bean("debateOllamaChatModel")
	public OllamaChatModel debateOllamaChatModel(
		@Value("${ai.debate.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.we-ai.com}}") String baseUrl,
		@Value("${ai.debate.model-name:${AI_DEBATE_MODEL_NAME:qwen2.5-coder}}") String modelName,
		@Value("${ai.debate.timeout:${AI_DEBATE_TIMEOUT:PT60S}}") Duration timeout
	) {
		return OllamaChatModel.builder()
			.baseUrl(baseUrl)
			.modelName(modelName)
			.temperature(0.2)
			.timeout(timeout)
			.build();
	}

	@Bean("oracleRagChatModel")
	public OllamaChatModel oracleRagChatModel(
		@Value("${ai.chat.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.we-ai.com}}") String baseUrl,
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
		@Value("${ai.chat.ollama-base-url:${OLLAMA_BASE_URL:https://ollama.we-ai.com}}") String baseUrl,
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

	@Bean("oracleContentRetriever")
	public ContentRetriever oracleContentRetriever(
		@org.springframework.beans.factory.annotation.Qualifier("oracleChromaEmbeddingStore")
		EmbeddingStore<TextSegment> embeddingStore,
		@org.springframework.beans.factory.annotation.Qualifier("oracleEmbeddingModel")
		EmbeddingModel embeddingModel,
		@Value("${ai.chat.retriever.max-results:4}") Integer maxResults,
		@Value("${ai.chat.retriever.min-score:0.65}") Double minScore
	) {
		return EmbeddingStoreContentRetriever.builder()
			.embeddingStore(embeddingStore)
			.embeddingModel(embeddingModel)
			.maxResults(maxResults)
			.minScore(minScore)
			.build();
	}
}
