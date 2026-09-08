package com.weai.server.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_ATTRIBUTE = "requestId";

	private static final int REQUEST_CACHE_LIMIT = 8_192;
	private static final int MAX_PAYLOAD_LENGTH = 2_000;
	private static final int MAX_HEADER_VALUE_LENGTH = 300;
	private static final Set<String> SENSITIVE_HEADERS = Set.of(
		HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ROOT),
		HttpHeaders.COOKIE.toLowerCase(Locale.ROOT),
		HttpHeaders.SET_COOKIE.toLowerCase(Locale.ROOT)
	);
	private static final Pattern SENSITIVE_FIELDS = Pattern.compile(
		"(?i)\"(password|token|accessToken|refreshToken|secret)\"\\s*:\\s*\"(.*?)\""
	);
	private static final Pattern PROJECT_API_PATH = Pattern.compile("^/api/v1/projects/(?<projectId>\\d+)(?:/.*)?$");
	private static final List<String> EXCLUDED_PATH_PREFIXES = List.of(
		"/swagger-ui",
		"/v3/api-docs",
		"/actuator",
		"/favicon.ico"
	);

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		return EXCLUDED_PATH_PREFIXES.stream().anyMatch(requestUri::startsWith)
			|| requestUri.matches("/api/v1/projects/[^/]+/server-logs/stream");
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);

		String requestId = UUID.randomUUID().toString().substring(0, 8);
		String projectId = extractProjectId(requestWrapper.getRequestURI());
		long startTime = System.currentTimeMillis();

		requestWrapper.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
		response.setHeader("X-Request-Id", requestId);

		MDC.put(ProjectLogContext.REQUEST_ID_KEY, requestId);
		if (projectId != null) {
			MDC.put(ProjectLogContext.PROJECT_ID_KEY, projectId);
		}
		try {
			filterChain.doFilter(requestWrapper, response);
		} finally {
			logExchange(requestWrapper, response, requestId, System.currentTimeMillis() - startTime);
			MDC.remove(ProjectLogContext.REQUEST_ID_KEY);
			MDC.remove(ProjectLogContext.PROJECT_ID_KEY);
		}
	}

	private String extractProjectId(String requestUri) {
		java.util.regex.Matcher matcher = PROJECT_API_PATH.matcher(requestUri);
		return matcher.matches() ? matcher.group("projectId") : null;
	}

	private void logExchange(
		ContentCachingRequestWrapper request,
		HttpServletResponse response,
		String requestId,
		long durationMs
	) {
		String requestUri = buildRequestUri(request);
		String requestHeaders = formatRequestHeaders(request);
		String requestBody = extractRequestBody(request);
		String responseHeaders = formatResponseHeaders(response);

		String logMessage = "[%s] %s %s status=%d durationMs=%d requestHeaders=%s requestBody=%s responseHeaders=%s"
			.formatted(
				requestId,
				request.getMethod(),
				requestUri,
				response.getStatus(),
				durationMs,
				requestHeaders,
				requestBody,
				responseHeaders
			);

		if (response.getStatus() >= 500) {
			log.error(logMessage);
			return;
		}

		if (response.getStatus() >= 400) {
			log.warn(logMessage);
			return;
		}

		log.info(logMessage);
	}

	private String formatRequestHeaders(ContentCachingRequestWrapper request) {
		Map<String, String> headers = new LinkedHashMap<>();
		Enumeration<String> headerNames = request.getHeaderNames();
		if (headerNames == null) {
			return "{}";
		}

		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			String headerValue = String.join(", ", Collections.list(request.getHeaders(headerName)));
			headers.put(headerName, maskHeaderValue(headerName, headerValue));
		}

		return headers.toString();
	}

	private String formatResponseHeaders(HttpServletResponse response) {
		Map<String, String> headers = new LinkedHashMap<>();
		for (String headerName : response.getHeaderNames()) {
			String headerValue = String.join(", ", response.getHeaders(headerName));
			headers.put(headerName, maskHeaderValue(headerName, headerValue));
		}
		return headers.toString();
	}

	private String maskHeaderValue(String headerName, String headerValue) {
		if (SENSITIVE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT))) {
			return "***";
		}
		return abbreviate(headerValue, MAX_HEADER_VALUE_LENGTH);
	}

	private String extractRequestBody(ContentCachingRequestWrapper request) {
		byte[] content = request.getContentAsByteArray();
		if (content.length == 0) {
			return "<empty>";
		}
		return formatBody(content, request.getContentType(), request.getCharacterEncoding());
	}

	private String formatBody(byte[] content, String contentType, String encoding) {
		if (!isTextBasedContentType(contentType)) {
			return "<binary %s (%d bytes)>".formatted(contentType == null ? "unknown" : contentType, content.length);
		}

		Charset charset = resolveCharset(contentType, encoding);
		String body = new String(content, charset);
		String sanitizedBody = SENSITIVE_FIELDS.matcher(body).replaceAll("\"$1\":\"***\"");
		return abbreviate(sanitizedBody, MAX_PAYLOAD_LENGTH);
	}

	private Charset resolveCharset(String contentType, String encoding) {
		if (StringUtils.hasText(contentType)) {
			try {
				MediaType mediaType = MediaType.parseMediaType(contentType);
				if (mediaType.getCharset() != null) {
					return mediaType.getCharset();
				}
				if (isUtf8ByDefault(mediaType)) {
					return StandardCharsets.UTF_8;
				}
			} catch (IllegalArgumentException ignored) {
				return StandardCharsets.UTF_8;
			}
		}

		if (StringUtils.hasText(encoding)) {
			return Charset.forName(encoding);
		}
		return StandardCharsets.UTF_8;
	}

	private boolean isUtf8ByDefault(MediaType mediaType) {
		return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)
			|| mediaType.isCompatibleWith(MediaType.APPLICATION_XML)
			|| mediaType.getType().equalsIgnoreCase("text");
	}

	private boolean isTextBasedContentType(String contentType) {
		if (!StringUtils.hasText(contentType)) {
			return true;
		}

		MediaType mediaType = MediaType.parseMediaType(contentType);
		return mediaType.getType().equalsIgnoreCase("text")
			|| mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)
			|| mediaType.isCompatibleWith(MediaType.APPLICATION_XML)
			|| mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED);
	}

	private String buildRequestUri(HttpServletRequest request) {
		String queryString = request.getQueryString();
		return queryString == null ? request.getRequestURI() : request.getRequestURI() + "?" + queryString;
	}

	private String abbreviate(String value, int maxLength) {
		if (!StringUtils.hasText(value) || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength) + "...(truncated)";
	}
}
