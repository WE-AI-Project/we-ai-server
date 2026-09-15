package com.weai.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Regression test for a real prod incident: application-prod.yml / application-stag.yml default
 * unset pattern env vars (e.g. APP_VERCEL_FRONTEND_PATTERN) to an empty string so the wildcard
 * stays opt-in (see WebSocketConfigTest). Spring Boot's @ConfigurationProperties validation runs
 * bean validation on the bound object, so if allowedOriginPatterns still carried a per-element
 * @NotBlank constraint, binding ["", "", ""] fails with "must not be blank" and the app refuses
 * to start entirely — which is exactly what happened in production.
 */
class AppWebPropertiesTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void allowedOriginPatternsAcceptsBlankEntriesLikeAnUnsetEnvVar() {
		AppWebProperties properties = new AppWebProperties();
		properties.getCors().setAllowedOriginPatterns(List.of("", "", ""));

		Set<ConstraintViolation<AppWebProperties>> violations = validator.validate(properties);

		assertThat(violations).isEmpty();
	}

	@Test
	void allowedOriginsStillRejectsBlankEntries() {
		AppWebProperties properties = new AppWebProperties();
		properties.getCors().setAllowedOrigins(List.of(""));

		Set<ConstraintViolation<AppWebProperties>> violations = validator.validate(properties);

		assertThat(violations).isNotEmpty();
	}
}
