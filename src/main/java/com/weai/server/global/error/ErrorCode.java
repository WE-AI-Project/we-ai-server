package com.weai.server.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// Common Errors
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400", "Invalid request input."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401", "Authentication is required."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "You do not have permission to access this resource."),
	CONFLICT(HttpStatus.CONFLICT, "COMMON_409", "The request conflicts with existing data."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "The requested resource could not be found."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "The HTTP method is not supported for this endpoint."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON_415", "The content type is not supported."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "An unexpected server error occurred."),

	// Auth & User Errors 추가
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_409_1", "이미 사용 중인 이메일입니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "가입되지 않은 이메일이거나 사용자를 찾을 수 없습니다."),
	INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "비밀번호가 일치하지 않습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}