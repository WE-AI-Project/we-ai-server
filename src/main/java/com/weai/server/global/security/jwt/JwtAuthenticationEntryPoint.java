package com.weai.server.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
		.findAndAddModules()
		.build();

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authenticationException
	) throws IOException {
		String message = (String) request.getAttribute(JwtAuthenticationFilter.AUTHENTICATION_FAILURE_REASON);
		if (!StringUtils.hasText(message)) {
			message = ErrorCode.UNAUTHORIZED.getMessage();
		}

		response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		OBJECT_MAPPER.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.UNAUTHORIZED, message));
	}
}
