package com.weai.server.domain.auth.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * {@code clientId}/{@code clientSecret}/{@code redirectUri} are intentionally NOT {@code @NotBlank}:
 * they used to be, with no default in application.yml, which meant the whole server - every
 * feature, not just social login - refused to boot with a {@code PlaceholderResolutionException}
 * on any environment (a fresh local dev checkout included) that had not set up Kakao/Naver/Google
 * OAuth credentials. A missing social-login credential is a per-provider feature gap, not a
 * security issue like a default DB password or JWT secret (see {@code DangerousDefaultSecretsGuard}
 * for those), so it now only logs a warning (see {@link #warnAboutUnconfiguredProviders()}) instead
 * of blocking startup; the provider's own authorization endpoint will reject an empty client_id if
 * that specific login is actually attempted.
 */
@Slf4j
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

	@Valid
	private final Kakao kakao = new Kakao();

	@Valid
	private final Naver naver = new Naver();

	@Valid
	private final Google google = new Google();

	@PostConstruct
	public void warnAboutUnconfiguredProviders() {
		List<String> unconfigured = new ArrayList<>();
		if (!StringUtils.hasText(kakao.getClientId())) {
			unconfigured.add("kakao (KAKAO_CLIENT_ID)");
		}
		if (!StringUtils.hasText(naver.getClientId())) {
			unconfigured.add("naver (NAVER_CLIENT_ID)");
		}
		if (!StringUtils.hasText(google.getClientId())) {
			unconfigured.add("google (GOOGLE_CLIENT_ID)");
		}
		if (!unconfigured.isEmpty()) {
			log.warn("Social login provider(s) not configured, that login method will fail if used: {}", unconfigured);
		}
	}

	@Getter
	@Setter
	public static class Kakao {

		private String clientId;

		private String redirectUri;

		@NotBlank
		private String authorizationUri = "https://kauth.kakao.com/oauth/authorize";

		@NotBlank
		private String tokenUri = "https://kauth.kakao.com/oauth/token";

		@NotBlank
		private String userInfoUri = "https://kapi.kakao.com/v2/user/me";
	}

	@Getter
	@Setter
	public static class Naver {

		private String clientId;

		private String clientSecret;

		private String redirectUri;

		@NotBlank
		private String authorizationUri = "https://nid.naver.com/oauth2.0/authorize";

		@NotBlank
		private String tokenUri = "https://nid.naver.com/oauth2.0/token";

		@NotBlank
		private String userInfoUri = "https://openapi.naver.com/v1/nid/me";
	}

	@Getter
	@Setter
	public static class Google {

		private String clientId;

		private String clientSecret;

		private String redirectUri;

		@NotBlank
		private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";

		@NotBlank
		private String tokenUri = "https://oauth2.googleapis.com/token";

		@NotBlank
		private String userInfoUri = "https://www.googleapis.com/oauth2/v2/userinfo";
	}
}
