package com.weai.server.domain.project.service;

public record BuildCommandResult(
	int exitCode,
	String output,
	String errorOutput
) {

	public boolean successful() {
		return exitCode == 0;
	}
}
