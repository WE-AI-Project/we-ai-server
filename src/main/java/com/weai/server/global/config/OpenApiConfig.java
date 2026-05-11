package com.weai.server.global.config;

import com.weai.server.global.swagger.SwaggerErrorResponseCustomizer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

	private final AppWebProperties appWebProperties;

	@Bean
	public OpenAPI weAiOpenApi() {
		return new OpenAPI()
			.servers(buildServers())
			.components(new Components()
				.addSecuritySchemes("bearerAuth", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")))
			.info(new Info()
				.title("WE AI Server API")
				.description("OpenAPI documentation for the WE AI backend service.")
				.version("v1")
				.contact(new Contact().name("WE AI Team"))
				.license(new License().name("Internal Use Only")));
	}

	@Bean
	public OperationCustomizer swaggerErrorResponseCustomizer() {
		return new SwaggerErrorResponseCustomizer();
	}

	private List<Server> buildServers() {
		String localBaseUrl = normalizeBaseUrl(appWebProperties.getLocalBaseUrl());
		String publicBaseUrl = normalizeBaseUrl(appWebProperties.getBaseUrl());
		Map<String, String> serverDescriptions = new LinkedHashMap<>();
		serverDescriptions.put(
			localBaseUrl,
			"Local test server (%s)".formatted(buildSwaggerUiUrl(localBaseUrl, "/swagger-ui.html"))
		);
		if (!localBaseUrl.equals(publicBaseUrl)) {
			serverDescriptions.put(
				publicBaseUrl,
				"Configured public server (%s)".formatted(buildSwaggerUiUrl(publicBaseUrl, "/swagger-ui/index.html"))
			);
		}

		return serverDescriptions.entrySet().stream()
			.map(entry -> new Server().url(entry.getKey()).description(entry.getValue()))
			.toList();
	}

	private String buildSwaggerUiUrl(String baseUrl, String swaggerPath) {
		return normalizeBaseUrl(baseUrl) + swaggerPath;
	}

	private String normalizeBaseUrl(String url) {
		String normalizedUrl = url;
		while (normalizedUrl.endsWith("/") && normalizedUrl.length() > 1) {
			normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
		}
		return normalizedUrl;
	}
}
