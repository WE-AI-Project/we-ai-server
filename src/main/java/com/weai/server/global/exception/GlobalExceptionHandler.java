package com.weai.server.global.exception;

import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
		return ResponseEntity.status(exception.getErrorCode().getStatus())
			.body(ApiResponse.failure(exception.getErrorCode(), exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
			.body(ApiResponse.failure(ErrorCode.INVALID_INPUT, extractValidationMessage(exception.getBindingResult())));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
		String message = exception.getParameterValidationResults().stream()
			.flatMap(result -> result.getResolvableErrors().stream())
			.map(error -> error.getDefaultMessage() == null ? "검증 오류가 발생했습니다." : error.getDefaultMessage())
			.collect(Collectors.joining(", "));

		return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
			.body(ApiResponse.failure(ErrorCode.INVALID_INPUT, message.isBlank() ? ErrorCode.INVALID_INPUT.getMessage() : message));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		String message = "%s 파라미터의 형식이 올바르지 않습니다.".formatted(exception.getName());
		return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
			.body(ApiResponse.failure(ErrorCode.INVALID_INPUT, message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	private String extractValidationMessage(BindingResult bindingResult) {
		String message = bindingResult.getFieldErrors().stream()
			.map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
			.collect(Collectors.joining(", "));
		return message.isBlank() ? ErrorCode.INVALID_INPUT.getMessage() : message;
	}
}
