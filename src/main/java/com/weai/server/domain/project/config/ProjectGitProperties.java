package com.weai.server.domain.project.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "project.git")
public class ProjectGitProperties {

	private String defaultBackendLocalPath = "";
	private String defaultFrontendLocalPath = "";
	private int defaultCommitLimit = 20;
	private int maxCommitLimit = 100;
}
