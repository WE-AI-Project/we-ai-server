package com.weai.server.domain.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "auth.verification")
public class AuthVerificationProperties {

	@Min(4)
	private int codeLength = 6;

	@NotNull
	private Duration expiration = Duration.ofMinutes(5);

	private boolean mockEnabled = false;

	private boolean exposeCodeInResponse = false;

	@NotBlank
	private String emailSubject = "[WE AI] Login verification code";

	private String emailFrom = "";

	@Valid
	private final KakaoTalk kakaoTalk = new KakaoTalk();

	@Getter
	@Setter
	public static class KakaoTalk {

		@NotBlank
		private String messageUri = "https://kapi.kakao.com/v2/api/talk/memo/default/send";
	}
}
