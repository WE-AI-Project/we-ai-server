package com.weai.server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.weai.server.global.swagger.SwaggerErrorResponseCustomizer;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI weAiOpenApi() {
		return new OpenAPI()

				.servers(List.of(
						new Server().url("http://we-ai.duckdns.org:18080").description("DuckDNS 배포 서버"),
						new Server().url("http://localhost:8080").description("로컬 테스트 서버")
				))

			.components(new Components()
				.addSecuritySchemes("bearerAuth", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")))
			.info(new Info()
				.title("WE AI Server API")
				.description("OpenAPI documentation for the WE AI backend boilerplate.")
				.version("v1")
				.contact(new Contact().name("WE AI Team"))
				.license(new License().name("Internal Use")));
	}

	@Bean
	public OperationCustomizer swaggerErrorResponseCustomizer() {
		return new SwaggerErrorResponseCustomizer();
	}
}
