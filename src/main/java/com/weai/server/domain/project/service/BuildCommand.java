package com.weai.server.domain.project.service;

import java.io.File;
import java.util.List;

public record BuildCommand(
	File workingDirectory,
	List<String> arguments,
	String display
) {
}
