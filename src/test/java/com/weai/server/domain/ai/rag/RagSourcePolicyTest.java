package com.weai.server.domain.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RagSourcePolicyTest {

	@ParameterizedTest
	@ValueSource(strings = {
		".env", ".env.local", "config/.env.production", "id_rsa", "certs/server.key", "keystore.jks",
		"secrets.yml", "client_secret.json", "credentials.txt", ".npmrc"
	})
	void detectsSecretFiles(String path) {
		assertThat(RagSourcePolicy.isSecretFile(path)).isTrue();
		assertThat(RagSourcePolicy.isIndexableWorkspaceFile(path)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"src/JwtSecretProvider.java", "SecurityConfig.java", "CredentialsForm.tsx"})
	void doesNotTreatCodeFilesNamedAfterSecretsAsSecrets(String path) {
		assertThat(RagSourcePolicy.isSecretFile(path)).isFalse();
		assertThat(RagSourcePolicy.isIndexableWorkspaceFile(path)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"package-lock.json", "logo.png", "vendor.min.js", "app.jar", "yarn.lock"})
	void skipsNonTextAndGeneratedFiles(String path) {
		assertThat(RagSourcePolicy.isIndexableWorkspaceFile(path)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Main.java", "README.md", "Dockerfile", "application.yml", "pom.xml", "page.tsx"})
	void indexesSourceAndDocFiles(String path) {
		assertThat(RagSourcePolicy.isIndexableWorkspaceFile(path)).isTrue();
	}

	@Test
	void masksCredentialValuesInConfigFilesButKeepsEnvPlaceholders() {
		String yaml = """
			spring:
			  datasource:
			    password: hunter2
			    username: weai
			jwt:
			  secret: ${JWT_SECRET:}
			api-key: "sk-live-123"
			""";

		String redacted = RagSourcePolicy.redactSecrets("src/main/resources/application.yml", yaml);

		assertThat(redacted)
			.contains("password: [REDACTED]")
			.contains("username: weai")
			.contains("secret: ${JWT_SECRET:}")
			.contains("api-key: [REDACTED]")
			.doesNotContain("hunter2")
			.doesNotContain("sk-live-123");
	}

	@Test
	void leavesSourceCodeUntouched() {
		String java = "String token = authHeader.substring(7);";

		assertThat(RagSourcePolicy.redactSecrets("JwtFilter.java", java)).isEqualTo(java);
	}
}
