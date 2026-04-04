package com.weai.server.global.security.config;

import com.weai.server.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

	private final DefaultAuthProperties authProperties;
	private final UserService userService;

	@Override
	public void run(ApplicationArguments args) {
		userService.ensureBootstrapAdmin(authProperties.getUsername(), authProperties.getPassword());
	}
}
