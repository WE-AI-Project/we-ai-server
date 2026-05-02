package com.weai.server.domain.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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

	@Getter
	@Setter
	public static class Kakao {

		@NotBlank
		private String clientId;

		@NotBlank
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

		@NotBlank
		private String clientId;

		@NotBlank
		private String clientSecret;

		@NotBlank
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

		@NotBlank
		private String clientId;

		@NotBlank
		private String clientSecret;

		@NotBlank
		private String redirectUri;

		@NotBlank
		private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";

		@NotBlank
		private String tokenUri = "https://oauth2.googleapis.com/token";

		@NotBlank
		private String userInfoUri = "https://www.googleapis.com/oauth2/v2/userinfo";
	}
}
