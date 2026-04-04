package com.weai.server.global.security.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Profile({"dev", "test"})
@Validated
@ConfigurationProperties(prefix = "app.auth")
public class DefaultAuthProperties {

	@NotBlank
	private String username;

	@NotBlank
	private String password;
}
