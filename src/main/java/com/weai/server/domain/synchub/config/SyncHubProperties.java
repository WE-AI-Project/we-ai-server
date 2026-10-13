package com.weai.server.domain.synchub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SyncHub = the Git repositories this server hosts, one bare repository per project
 * ({@code {root}/{projectId}.git}). The desktop app pushes to it over Git Smart HTTP.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.synchub")
public class SyncHubProperties {

	/** Directory holding the bare repositories. In Docker this lives on the mounted /workspace volume. */
	private String root = "/workspace/synchub";

	/** Branch HEAD points at when a repository is created. */
	private String defaultBranch = "main";

	/** Remote name the desktop app registers for the SyncHub URL. */
	private String remoteName = "synchub";

	private int defaultCommitLimit = 20;
	private int maxCommitLimit = 100;

	/** Largest pack a single push may upload. */
	private long maxPushSizeBytes = 200L * 1024 * 1024;
}
