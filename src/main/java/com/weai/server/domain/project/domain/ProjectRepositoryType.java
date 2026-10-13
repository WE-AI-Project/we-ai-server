package com.weai.server.domain.project.domain;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.Locale;

public enum ProjectRepositoryType {
	BACKEND,
	FRONTEND;

	public static ProjectRepositoryType from(String rawValue) {
		if (rawValue == null || rawValue.isBlank()) {
			return BACKEND;
		}

		try {
			return valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_PROJECT_REPOSITORY_TYPE);
		}
	}
}
