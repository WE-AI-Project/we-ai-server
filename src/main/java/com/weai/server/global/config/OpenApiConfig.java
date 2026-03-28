package com.weai.server.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI weAiOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("WE AI Server API")
				.description("WE AI 백엔드 보일러플레이트의 OpenAPI 문서입니다.")
				.version("v1")
				.contact(new Contact()
					.name("WE AI Team"))
				.license(new License()
					.name("Internal Use")));
	}
}
