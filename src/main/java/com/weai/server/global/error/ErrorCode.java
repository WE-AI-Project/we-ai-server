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

	// Auth & User Errors
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_409_1", "The email address is already in use."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "The requested user could not be found."),
	INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "The password does not match."),

	// Project Errors
	PROJECT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_1", "프로젝트명은 필수입니다."),
	PROJECT_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "PROJECT_400_2", "프로젝트명은 50자 이하여야 합니다."),
	INVALID_PROJECT_DATE(HttpStatus.BAD_REQUEST, "PROJECT_400_3", "마감일은 오늘보다 이전일 수 없습니다."),
	PROJECT_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_4", "참여 코드는 필수입니다."),
	INVALID_PROJECT_CODE_FORMAT(HttpStatus.BAD_REQUEST, "PROJECT_400_5", "참여 코드는 영문 대문자와 숫자 8자리여야 합니다."),
	PROJECT_PATH_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_6", "프로젝트 저장 위치는 필수입니다."),
	PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_1", "프로젝트를 찾을 수 없습니다."),
	PROJECT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "PROJECT_403_1", "비활성화된 프로젝트에는 참여할 수 없습니다."),
	ALREADY_JOINED_PROJECT(HttpStatus.CONFLICT, "PROJECT_409_1", "이미 참여 중인 프로젝트입니다."),
	PROJECT_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_1", "참여 코드 생성에 실패했습니다."),
	PROJECT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_2", "프로젝트 생성에 실패했습니다."),
	PROJECT_JOIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_3", "프로젝트 참여에 실패했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
