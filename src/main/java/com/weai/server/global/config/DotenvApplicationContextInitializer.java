package com.weai.server.global.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DotenvApplicationContextInitializer
	implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotenvPropertySource";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment environment = applicationContext.getEnvironment();

		Dotenv dotenv = Dotenv.configure()
			.directory(System.getProperty("user.dir"))
			.ignoreIfMalformed()
			.ignoreIfMissing()
			.load();

		Map<String, Object> dotenvProperties = new LinkedHashMap<>();
		dotenv.entries().forEach(entry -> dotenvProperties.put(entry.getKey(), entry.getValue()));

		if (!dotenvProperties.isEmpty()) {
			environment.getPropertySources().addLast(
				new MapPropertySource(DOTENV_PROPERTY_SOURCE_NAME, dotenvProperties)
			);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}
