package com.weai.server.global.security.jwt;

import com.weai.server.domain.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private final JwtProperties jwtProperties;

	public String createAccessToken(User user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(jwtProperties.getAccessTokenExpiration());

		return Jwts.builder()
			.subject(user.getEmail())
			.issuer(jwtProperties.getIssuer())
			.issuedAt(Date.from(issuedAt))
			.expiration(Date.from(expiresAt))
			.claim("email", user.getEmail())
			.claim("role", user.getRole().name())
			.signWith(getSigningKey())
			.compact();
	}

	public Authentication getAuthentication(String token) {
		Claims claims = parseClaims(token);
		String email = claims.get("email", String.class);
		String role = claims.get("role", String.class);

		Assert.isTrue(StringUtils.hasText(email), "JWT email claim is required.");
		Assert.isTrue(StringUtils.hasText(role), "JWT role claim is required.");

		GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
		java.util.List<GrantedAuthority> authorities = java.util.List.of(authority);

		org.springframework.security.core.userdetails.User principal =
			new org.springframework.security.core.userdetails.User(email, "", authorities);
		return new UsernamePasswordAuthenticationToken(principal, token, authorities);
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException exception) {
			return false;
		}
	}

	public long getAccessTokenExpirationSeconds() {
		return jwtProperties.getAccessTokenExpiration().getSeconds();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(getSigningKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		Assert.isTrue(keyBytes.length >= 32, "JWT secret must be at least 32 bytes long.");
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
