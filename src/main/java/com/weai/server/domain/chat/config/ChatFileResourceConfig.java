package com.weai.server.domain.chat.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ChatFileResourceConfig implements WebMvcConfigurer {

	private final Path uploadRoot;

	public ChatFileResourceConfig(@Value("${chat.file.upload-root:uploads/chat}") String uploadRoot) {
		this.uploadRoot = Paths.get(uploadRoot).toAbsolutePath().normalize();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/chat/**")
			.addResourceLocations(uploadRoot.toUri().toString());
	}
}
