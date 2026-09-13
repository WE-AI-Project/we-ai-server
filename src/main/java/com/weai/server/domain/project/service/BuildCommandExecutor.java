package com.weai.server.domain.project.service;

public interface BuildCommandExecutor {

	BuildCommandResult execute(BuildCommand command);
}
