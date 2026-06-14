package com.weai.server.global.config;

import com.weai.server.domain.auth.config.AuthVerificationProperties;
import com.weai.server.domain.auth.config.OAuthProperties;
import com.weai.server.domain.project.config.ProjectGitProperties;
import com.weai.server.global.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
	AppWebProperties.class,
	JwtProperties.class,
	AuthVerificationProperties.class,
	OAuthProperties.class,
	ProjectGitProperties.class
})
public class PropertiesConfig {
}
