package com.weai.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class ApplicationDevConfigurationTest {

	@Test
	void devProfileCorsAllowsViteFallbackPort() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(
			new ClassPathResource("application.yml"),
			new ClassPathResource("application-dev.yml")
		);

		Properties properties = factory.getObject();

		assertThat(properties).isNotNull();
		assertThat(properties.getProperty("app.web.cors.allowed-origins[0]"))
			.isEqualTo("http://localhost:3000");
		assertThat(properties.getProperty("app.web.cors.allowed-origins[1]"))
			.isEqualTo("http://127.0.0.1:3000");
		assertThat(properties.getProperty("app.web.cors.allowed-origins[2]"))
			.isEqualTo("http://localhost:5173");
		assertThat(properties.getProperty("app.web.cors.allowed-origins[3]"))
			.isEqualTo("http://localhost:5174");
	}
}
