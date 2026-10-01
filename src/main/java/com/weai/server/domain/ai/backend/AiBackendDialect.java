package com.weai.server.domain.ai.backend;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;

public enum AiBackendDialect {
	OLLAMA_NATIVE,
	OPENAI_COMPATIBLE;

	public static AiBackendDialect from(String raw) {
		if (raw == null || raw.isBlank()) {
			return OLLAMA_NATIVE;
		}
		try {
			return AiBackendDialect.valueOf(raw.trim().toUpperCase().replace('-', '_'));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.AI_BACKEND_INVALID_DIALECT);
		}
	}
}
