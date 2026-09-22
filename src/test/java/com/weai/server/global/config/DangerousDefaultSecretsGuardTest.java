package com.weai.server.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

class DangerousDefaultSecretsGuardTest {

	private static final String REAL_MASTER_KEY = "a-real-base64-encoded-32-byte-key==";

	@Test
	void refusesToStartWithDefaultDbPasswordOutsideDevTest() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");

		assertThatThrownBy(() -> new DangerousDefaultSecretsGuard(
			environment, "change-me-db-password", "a-real-secret", "a-real-secret", REAL_MASTER_KEY
		).validate())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("spring.datasource.password");
	}

	@Test
	void refusesToStartWithDefaultJwtSecretOutsideDevTest() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("stag");

		assertThatThrownBy(() -> new DangerousDefaultSecretsGuard(
			environment, "a-real-password", "change-this-development-secret-key-at-least-32-bytes", "a-real-secret", REAL_MASTER_KEY
		).validate())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("spring.jwt.secret");
	}

	@Test
	void refusesToStartWithDefaultMinioSecretOutsideDevTest() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");

		assertThatThrownBy(() -> new DangerousDefaultSecretsGuard(
			environment, "a-real-password", "a-real-secret", "change-me-minio-password", REAL_MASTER_KEY
		).validate())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("storage.minio.secret-key");
	}

	@Test
	void refusesToStartWithBlankEnvironmentMasterKeyOutsideDevTest() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");

		assertThatThrownBy(() -> new DangerousDefaultSecretsGuard(
			environment, "a-real-password", "a-real-secret", "a-real-secret", ""
		).validate())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("synaipse.environment.master-key");
	}

	@Test
	void refusesToStartWhenNoProfileIsActiveAtAll() {
		StandardEnvironment environment = new StandardEnvironment();

		assertThatThrownBy(() -> new DangerousDefaultSecretsGuard(
			environment, "change-me-db-password", "a-real-secret", "a-real-secret", REAL_MASTER_KEY
		).validate())
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void allowsPlaceholderValuesUnderDevOrTestProfiles() {
		StandardEnvironment dev = new StandardEnvironment();
		dev.setActiveProfiles("dev");
		assertThatCode(() -> new DangerousDefaultSecretsGuard(
			dev, "change-me-db-password", "change-this-development-secret-key-at-least-32-bytes", "change-me-minio-password", ""
		).validate()).doesNotThrowAnyException();

		StandardEnvironment test = new StandardEnvironment();
		test.setActiveProfiles("test");
		assertThatCode(() -> new DangerousDefaultSecretsGuard(
			test, "change-me-db-password", "change-this-development-secret-key-at-least-32-bytes", "change-me-minio-password", ""
		).validate()).doesNotThrowAnyException();
	}

	@Test
	void allowsRealValuesOutsideDevTest() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");

		assertThatCode(() -> new DangerousDefaultSecretsGuard(
			environment, "a-real-password", "a-real-jwt-secret", "a-real-minio-secret", REAL_MASTER_KEY
		).validate()).doesNotThrowAnyException();
	}
}
