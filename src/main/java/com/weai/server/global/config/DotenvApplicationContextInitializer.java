package com.weai.server.global.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DotenvApplicationContextInitializer
	implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotenvPropertySource";
	private static final String BASE_DOTENV_FILE_NAME = ".env";
	private static final String PROFILE_DOTENV_FILE_PREFIX = ".env.";
	private static final String APP_PROFILE_PROPERTY_NAME = "APP_PROFILE";
	private static final String SPRING_ACTIVE_PROFILE_ENV_NAME = "SPRING_PROFILES_ACTIVE";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		Map<String, Object> dotenvProperties = loadDotenvProperties(BASE_DOTENV_FILE_NAME);

		resolveActiveProfiles(environment, dotenvProperties).forEach(profile ->
			dotenvProperties.putAll(loadDotenvProperties(PROFILE_DOTENV_FILE_PREFIX + profile))
		);

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

	private Map<String, Object> loadDotenvProperties(String fileName) {
		Dotenv dotenv = Dotenv.configure()
			.directory(System.getProperty("user.dir"))
			.filename(fileName)
			.ignoreIfMalformed()
			.ignoreIfMissing()
			.load();

		Map<String, Object> dotenvProperties = new LinkedHashMap<>();
		dotenv.entries().forEach(entry -> dotenvProperties.put(entry.getKey(), entry.getValue()));
		return dotenvProperties;
	}

	private Set<String> resolveActiveProfiles(
		ConfigurableEnvironment environment,
		Map<String, Object> dotenvProperties
	) {
		Set<String> activeProfiles = new LinkedHashSet<>();

		addProfiles(activeProfiles, environment.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME));
		addProfiles(activeProfiles, environment.getProperty(APP_PROFILE_PROPERTY_NAME));
		addProfiles(activeProfiles, asString(dotenvProperties.get(SPRING_ACTIVE_PROFILE_ENV_NAME)));
		addProfiles(activeProfiles, asString(dotenvProperties.get(APP_PROFILE_PROPERTY_NAME)));

		return activeProfiles;
	}

	private void addProfiles(Set<String> activeProfiles, String rawProfiles) {
		if (!StringUtils.hasText(rawProfiles)) {
			return;
		}

		Arrays.stream(rawProfiles.split(","))
			.map(String::trim)
			.filter(StringUtils::hasText)
			.forEach(activeProfiles::add);
	}

	private String asString(Object value) {
		return value instanceof String stringValue ? stringValue : null;
	}
}
