package com.weai.server.domain.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

class AuthVerificationSecurityGuardTest {

	@Test
	void refusesToStartWhenMockEnabledOutsideDevOrTest() {
		AuthVerificationProperties properties = new AuthVerificationProperties();
		properties.setMockEnabled(true);
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");

		assertThatThrownBy(() -> new AuthVerificationSecurityGuard(properties, environment).validate())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("mock-enabled");
	}

	@Test
	void refusesToStartWhenCodeExposedOutsideDevOrTest() {
		AuthVerificationProperties properties = new AuthVerificationProperties();
		properties.setExposeCodeInResponse(true);
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("stag");

		assertThatThrownBy(() -> new AuthVerificationSecurityGuard(properties, environment).validate())
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void refusesToStartWhenNoProfileIsActiveAtAll() {
		AuthVerificationProperties properties = new AuthVerificationProperties();
		properties.setMockEnabled(true);
		StandardEnvironment environment = new StandardEnvironment();

		assertThatThrownBy(() -> new AuthVerificationSecurityGuard(properties, environment).validate())
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void allowsMockEnabledUnderDevProfile() {
		AuthVerificationProperties properties = new AuthVerificationProperties();
		properties.setMockEnabled(true);
		properties.setExposeCodeInResponse(true);
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("dev");

		new AuthVerificationSecurityGuard(properties, environment).validate();
		assertThat(properties.isMockEnabled()).isTrue();
	}

	@Test
	void allowsDisabledFlagsInProd() {
		AuthVerificationProperties properties = new AuthVerificationProperties();
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");

		new AuthVerificationSecurityGuard(properties, environment).validate();
	}
}
