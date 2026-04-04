package com.weai.server.global.security.jwt;

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
@ConfigurationProperties(prefix = "spring.jwt")
public class JwtProperties {

	@NotBlank
	private String secret;

	@NotBlank
	private String issuer;

	@NotNull
	private Duration accessTokenExpiration = Duration.ofHours(1);

	@NotNull
	private Duration refreshTokenExpiration = Duration.ofDays(7);
}
