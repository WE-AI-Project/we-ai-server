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
	PROJECT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_1", "Project name is required."),
	PROJECT_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "PROJECT_400_2", "Project name must be 50 characters or fewer."),
	INVALID_PROJECT_DATE(HttpStatus.BAD_REQUEST, "PROJECT_400_3", "Target date cannot be earlier than the start date."),
	PROJECT_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "PROJECT_400_4", "Project code is required."),
	INVALID_PROJECT_CODE_FORMAT(HttpStatus.BAD_REQUEST, "PROJECT_400_5", "Project code must be 8 uppercase letters or digits."),
	PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_404_1", "The requested project could not be found."),
	PROJECT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "PROJECT_403_1", "This project is not active."),
	ALREADY_JOINED_PROJECT(HttpStatus.CONFLICT, "PROJECT_409_1", "You have already joined this project."),
	PROJECT_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_1", "Failed to generate a project code."),
	PROJECT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_2", "Failed to create the project."),
	PROJECT_JOIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROJECT_500_3", "Failed to join the project.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
