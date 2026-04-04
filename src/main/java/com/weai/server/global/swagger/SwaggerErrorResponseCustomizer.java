package com.weai.server.global.swagger;

import com.weai.server.global.error.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

public class SwaggerErrorResponseCustomizer implements OperationCustomizer {

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		Set<ErrorCode> errorCodes = collectErrorCodes(handlerMethod);
		if (errorCodes.isEmpty()) {
			return operation;
		}

		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}

		for (ErrorCode errorCode : errorCodes) {
			String httpStatusCode = String.valueOf(errorCode.getStatus().value());
			io.swagger.v3.oas.models.responses.ApiResponse apiResponse = responses.containsKey(httpStatusCode)
				? responses.get(httpStatusCode)
				: new io.swagger.v3.oas.models.responses.ApiResponse();

			if (apiResponse.getDescription() == null || apiResponse.getDescription().isBlank()) {
				apiResponse.setDescription(errorCode.getMessage());
			}

			Content content = apiResponse.getContent() == null ? new Content() : apiResponse.getContent();
			MediaType mediaType = content.containsKey(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
				? content.get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
				: new MediaType();

			if (mediaType.getSchema() == null) {
				mediaType.setSchema(new ObjectSchema());
			}
			mediaType.setExample(buildExample(errorCode));
			content.addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType);
			apiResponse.setContent(content);
			responses.addApiResponse(httpStatusCode, apiResponse);
		}

		return operation;
	}

	private Set<ErrorCode> collectErrorCodes(HandlerMethod handlerMethod) {
		Set<ErrorCode> errorCodes = new LinkedHashSet<>();

		SwaggerErrorResponses classAnnotation = AnnotatedElementUtils.findMergedAnnotation(
			handlerMethod.getBeanType(),
			SwaggerErrorResponses.class
		);
		if (classAnnotation != null) {
			errorCodes.addAll(Set.of(classAnnotation.value()));
		}

		SwaggerErrorResponses methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
			handlerMethod.getMethod(),
			SwaggerErrorResponses.class
		);
		if (methodAnnotation != null) {
			errorCodes.addAll(Set.of(methodAnnotation.value()));
		}

		return errorCodes;
	}

	private Map<String, Object> buildExample(ErrorCode errorCode) {
		return Map.of(
			"success", false,
			"code", errorCode.getCode(),
			"message", errorCode.getMessage(),
			"timestamp", "2026-04-04T00:00:00"
		);
	}
}
