package com.weai.server.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start the application if Swagger/OpenAPI ({@code springdoc.api-docs.enabled} or
 * {@code springdoc.swagger-ui.enabled}) is turned on under the {@code prod} profile.
 *
 * {@code /swagger-ui.html}, {@code /swagger-ui/**}, and {@code /v3/api-docs/**} are permitted
 * without authentication in {@link com.weai.server.global.security.config.SecurityConfig}, so a
 * misconfigured {@code SWAGGER_ENABLED=true} in a prod {@code .env} would publicly expose the
 * entire API surface/schema. application-prod.yml already defaults this to false, but an operator
 * can still override it via the env var — fail loudly instead of silently shipping it.
 */
@Component
public class SwaggerSecurityGuard {

	private static final String PROD_PROFILE = "prod";

	private final Environment environment;
	private final boolean apiDocsEnabled;
	private final boolean swaggerUiEnabled;

	public SwaggerSecurityGuard(
		Environment environment,
		@Value("${springdoc.api-docs.enabled:true}") boolean apiDocsEnabled,
		@Value("${springdoc.swagger-ui.enabled:true}") boolean swaggerUiEnabled
	) {
		this.environment = environment;
		this.apiDocsEnabled = apiDocsEnabled;
		this.swaggerUiEnabled = swaggerUiEnabled;
	}

	@PostConstruct
	public void validate() {
		boolean runningInProd = java.util.Arrays.asList(environment.getActiveProfiles()).contains(PROD_PROFILE);
		if (!runningInProd) {
			return;
		}

		if (apiDocsEnabled || swaggerUiEnabled) {
			throw new IllegalStateException(
				"springdoc.api-docs.enabled and springdoc.swagger-ui.enabled must stay false under the "
					+ "'prod' profile. Refusing to start with SWAGGER_ENABLED (or an equivalent override) "
					+ "turned on, to avoid publicly exposing the API schema/UI in production."
			);
		}
	}
}
