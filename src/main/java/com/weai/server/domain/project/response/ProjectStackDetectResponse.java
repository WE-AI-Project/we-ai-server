package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectTechStackCategory;
import java.util.List;

public record ProjectStackDetectResponse(
	String localPath,
	List<String> stack,
	String framework,
	String language,
	String build,
	List<DetectedTechStack> techStacks,
	List<String> detectedFiles
) {
	public record DetectedTechStack(
		String name,
		String version,
		ProjectTechStackCategory category,
		boolean isRequired
	) {
	}
}
