package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weai.server.domain.project.response.ProjectStackDetectResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectStackDetectionServiceTest {

	@TempDir
	Path projectRoot;

	@Test
	void detectsNodeAndFrontendStackFromPackageJson() throws IOException {
		Files.writeString(projectRoot.resolve("package.json"), """
			{
			  "engines": { "node": ">=20" },
			  "dependencies": { "react": "^18.3.1" },
			  "devDependencies": { "typescript": "^5.4.0", "vite": "^6.3.5" }
			}
			""");

		ProjectStackDetectResponse result = new ProjectStackDetectionService(new ObjectMapper())
			.detect(projectRoot.toString());

		assertThat(result.stack()).contains("Node.js 20", "React 18.3.1", "TypeScript 5.4.0", "Vite 6.3.5");
		assertThat(result.detectedFiles()).containsExactly("package.json");
		assertThat(result.framework()).contains("React");
	}

	@Test
	void ignoresGeneratedDependencyDirectories() throws IOException {
		Path ignored = Files.createDirectories(projectRoot.resolve("node_modules/example"));
		Files.writeString(ignored.resolve("package.json"), "{\"dependencies\":{\"react\":\"99.0.0\"}}");

		ProjectStackDetectResponse result = new ProjectStackDetectionService(new ObjectMapper())
			.detect(projectRoot.toString());

		assertThat(result.stack()).isEmpty();
		assertThat(result.detectedFiles()).isEmpty();
	}
}
