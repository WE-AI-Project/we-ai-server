package com.weai.server.global.config;

import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

	@Bean
	public OllamaChatModel ollamaChatModel(
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
}
