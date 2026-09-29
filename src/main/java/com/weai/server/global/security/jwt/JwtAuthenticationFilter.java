package com.weai.server.global.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTHENTICATION_FAILURE_REASON = "authenticationFailureReason";

	/**
	 * A native browser {@code EventSource} cannot set the {@code Authorization} header, so this one
	 * endpoint structurally needs a query-param fallback. This used to be accepted on every request
	 * (CWE-598: any endpoint could be authenticated via {@code ?token=}, leaking access tokens into
	 * server access logs, proxy logs, and browser history) - it is now confined to exactly the path
	 * that needs it instead of being a blanket alternative to the Authorization header.
	 */
	private static final java.util.regex.Pattern QUERY_TOKEN_ALLOWED_PATH =
		java.util.regex.Pattern.compile("^/api/v1/projects/\\d+/server-logs/stream$");

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String token = resolveAccessToken(request, request.getRequestURI());

		if (StringUtils.hasText(token)) {
			try {
				SecurityContextHolder.getContext().setAuthentication(jwtTokenProvider.getAuthentication(token));
			} catch (JwtException | IllegalArgumentException exception) {
				SecurityContextHolder.clearContext();
				request.setAttribute(AUTHENTICATION_FAILURE_REASON, "Invalid or expired access token.");
			}
		}

		filterChain.doFilter(request, response);
	}

	private String resolveAccessToken(HttpServletRequest request, String requestUri) {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
			return authorizationHeader.substring(7);
		}

		if (!QUERY_TOKEN_ALLOWED_PATH.matcher(requestUri).matches()) {
			return null;
		}

		String tokenParam = request.getParameter("token");
		if (StringUtils.hasText(tokenParam)) {
			return tokenParam.trim();
		}

		String accessTokenParam = request.getParameter("accessToken");
		if (StringUtils.hasText(accessTokenParam)) {
			return accessTokenParam.trim();
		}

		return null;
	}
}
