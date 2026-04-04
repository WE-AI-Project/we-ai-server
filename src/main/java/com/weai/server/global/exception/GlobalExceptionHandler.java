package com.weai.server.global.exception;

import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception, HttpServletRequest request) {
		return logClientError(exception.getErrorCode(), exception.getMessage(), exception, request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		return logClientError(
			ErrorCode.INVALID_INPUT,
			extractValidationMessage(exception.getBindingResult()),
			exception,
			request
		);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
		HandlerMethodValidationException exception,
		HttpServletRequest request
	) {
		String message = exception.getParameterValidationResults().stream()
			.flatMap(result -> result.getResolvableErrors().stream())
			.map(error -> error.getDefaultMessage() == null ? "Validation failed." : error.getDefaultMessage())
			.collect(Collectors.joining(", "));

		return logClientError(
			ErrorCode.INVALID_INPUT,
			message.isBlank() ? ErrorCode.INVALID_INPUT.getMessage() : message,
			exception,
			request
		);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
		MethodArgumentTypeMismatchException exception,
		HttpServletRequest request
	) {
		String message = "Parameter '%s' has an invalid format.".formatted(exception.getName());
		return logClientError(ErrorCode.INVALID_INPUT, message, exception, request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
		MissingServletRequestParameterException exception,
		HttpServletRequest request
	) {
		String message = "Required parameter '%s' is missing.".formatted(exception.getParameterName());
		return logClientError(ErrorCode.INVALID_INPUT, message, exception, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
		HttpMessageNotReadableException exception,
		HttpServletRequest request
	) {
		return logClientError(ErrorCode.INVALID_INPUT, "Request body could not be parsed.", exception, request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
		AuthenticationException exception,
		HttpServletRequest request
	) {
		return logClientError(ErrorCode.UNAUTHORIZED, "Invalid username or password.", exception, request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
		HttpRequestMethodNotSupportedException exception,
		HttpServletRequest request
	) {
		String message = "HTTP method '%s' is not supported for this endpoint.".formatted(exception.getMethod());
		return logClientError(ErrorCode.METHOD_NOT_ALLOWED, message, exception, request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
		HttpMediaTypeNotSupportedException exception,
		HttpServletRequest request
	) {
		String contentType = exception.getContentType() == null ? "unknown" : exception.getContentType().toString();
		String message = "Content type '%s' is not supported.".formatted(contentType);
		return logClientError(ErrorCode.UNSUPPORTED_MEDIA_TYPE, message, exception, request);
	}

	@ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
	public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception exception, HttpServletRequest request) {
		return logClientError(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getMessage(), exception, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception exception, HttpServletRequest request) {
		String requestSummary = request.getMethod() + " " + buildRequestUri(request);
		log.error("Unhandled exception while processing {}", requestSummary, exception);
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	private ResponseEntity<ApiResponse<Void>> logClientError(
		ErrorCode errorCode,
		String message,
		Exception exception,
		HttpServletRequest request
	) {
		String requestSummary = request.getMethod() + " " + buildRequestUri(request);
		log.warn("Handled client error on {} -> {} ({}): {}",
			requestSummary,
			errorCode.getCode(),
			exception.getClass().getSimpleName(),
			message);

		return ResponseEntity.status(errorCode.getStatus())
			.body(ApiResponse.failure(errorCode, message));
	}

	private String extractValidationMessage(BindingResult bindingResult) {
		String message = bindingResult.getFieldErrors().stream()
			.map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
			.collect(Collectors.joining(", "));
		return message.isBlank() ? ErrorCode.INVALID_INPUT.getMessage() : message;
	}

	private String buildRequestUri(HttpServletRequest request) {
		String queryString = request.getQueryString();
		return queryString == null ? request.getRequestURI() : request.getRequestURI() + "?" + queryString;
	}
}
