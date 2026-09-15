package com.weai.server.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

class SwaggerSecurityGuardTest {

	@Test
	void refusesToStartWhenApiDocsEnabledInProd() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");

		assertThatThrownBy(() -> new SwaggerSecurityGuard(environment, true, false).validate())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("prod");
	}

	@Test
	void refusesToStartWhenSwaggerUiEnabledInProd() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");

		assertThatThrownBy(() -> new SwaggerSecurityGuard(environment, false, true).validate())
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void allowsSwaggerInProdWhenBothDisabled() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");

		assertThatCode(() -> new SwaggerSecurityGuard(environment, false, false).validate())
			.doesNotThrowAnyException();
	}

	@Test
	void allowsSwaggerEnabledUnderStagOrDevProfiles() {
		StandardEnvironment stag = new StandardEnvironment();
		stag.setActiveProfiles("stag");
		assertThatCode(() -> new SwaggerSecurityGuard(stag, true, true).validate())
			.doesNotThrowAnyException();

		StandardEnvironment dev = new StandardEnvironment();
		dev.setActiveProfiles("dev");
		assertThatCode(() -> new SwaggerSecurityGuard(dev, true, true).validate())
			.doesNotThrowAnyException();
	}
}
