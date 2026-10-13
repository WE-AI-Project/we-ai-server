package com.weai.server.domain.ai.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.exception.LangChain4jException;
import dev.langchain4j.exception.ModelNotFoundException;
import dev.langchain4j.exception.TimeoutException;
import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AiCallRetrierTest {

	@Test
	void cloudflareTunnelDownBecomesServiceUnavailableAfterRetries() {
		AtomicInteger attempts = new AtomicInteger();

		assertThatThrownBy(() -> AiCallRetrier.withRetry("AI chat", 2, 1, () -> {
			attempts.incrementAndGet();
			throw new InternalServerException("error code: 1033");
		}))
			.isInstanceOf(ApiException.class)
			.satisfies(exception -> {
				ApiException apiException = (ApiException) exception;
				assertThat(apiException.getErrorCode()).isEqualTo(ErrorCode.AI_SERVER_UNAVAILABLE);
				assertThat(apiException.getErrorCode().getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
				assertThat(apiException.getMessage()).isEqualTo("AI 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				assertThat(apiException.getCause()).isInstanceOf(InternalServerException.class);
			});
		assertThat(attempts).hasValue(2);
	}

	@Test
	void connectionRefusedAndTimeoutsAreServiceUnavailable() {
		assertThatThrownBy(() -> AiCallRetrier.withRetry("AI QA", 1, 1, () -> {
			throw new LangChain4jException(new ConnectException("Connection refused"));
		}))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.AI_SERVER_UNAVAILABLE);

		assertThatThrownBy(() -> AiCallRetrier.withRetry("AI commit", 1, 1, () -> {
			throw new TimeoutException("timed out");
		}))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.AI_SERVER_UNAVAILABLE);
	}

	@Test
	void otherFailuresAreRethrownUnchanged() {
		IllegalStateException unrelated = new IllegalStateException("boom", new ConnectException("not from langchain4j"));
		assertThatThrownBy(() -> AiCallRetrier.withRetry("AI chat", 1, 1, () -> {
			throw unrelated;
		})).isSameAs(unrelated);

		ModelNotFoundException modelNotFound = new ModelNotFoundException("model 'x' not found");
		assertThatThrownBy(() -> AiCallRetrier.withRetry("AI chat", 1, 1, () -> {
			throw modelNotFound;
		})).isSameAs(modelNotFound);

		InvalidRequestException invalidRequest = new InvalidRequestException("bad request");
		assertThatThrownBy(() -> AiCallRetrier.withRetry("AI chat", 1, 1, () -> {
			throw invalidRequest;
		})).isSameAs(invalidRequest);
	}

	@Test
	void successfulRetryReturnsValue() {
		AtomicInteger attempts = new AtomicInteger();
		String result = AiCallRetrier.withRetry("AI chat", 2, 1, () -> {
			if (attempts.incrementAndGet() == 1) {
				throw new InternalServerException("blip");
			}
			return "ok";
		});
		assertThat(result).isEqualTo("ok");
	}
}
