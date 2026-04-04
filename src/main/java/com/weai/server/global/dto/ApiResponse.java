package com.weai.server.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weai.server.global.error.ErrorCode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

	private final boolean success;
	private final String code;
	private final String message;
	private final T data;
	private final LocalDateTime timestamp;

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, "SUCCESS", "Request completed successfully.", data, LocalDateTime.now());
	}

	public static ApiResponse<Void> successMessage(String message) {
		return new ApiResponse<>(true, "SUCCESS", message, null, LocalDateTime.now());
	}

	public static ApiResponse<Void> failure(ErrorCode errorCode) {
		return failure(errorCode, errorCode.getMessage());
	}

	public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
		return new ApiResponse<>(false, errorCode.getCode(), message, null, LocalDateTime.now());
	}
}
