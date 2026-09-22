package com.weai.server.global.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start the application outside the {@code dev}/{@code test} profiles if a real value
 * was never supplied for a security-sensitive setting and it is still sitting on its well-known
 * placeholder default (e.g. {@code change-me-db-password}). Those defaults exist purely so local
 * development works out of the box; silently booting a real deployment with a guessable DB
 * password or JWT signing key is a full compromise waiting to happen, so this fails loudly at
 * startup instead of the server running "successfully" in a broken-secure state.
 */
@Component
public class DangerousDefaultSecretsGuard {

	private static final Set<String> SAFE_PROFILES = Set.of("dev", "test");

	private static final String DEFAULT_DB_PASSWORD = "change-me-db-password";
	private static final String DEFAULT_JWT_SECRET = "change-this-development-secret-key-at-least-32-bytes";
	private static final String DEFAULT_MINIO_SECRET_KEY = "change-me-minio-password";

	private final Environment environment;
	private final String dbPassword;
	private final String jwtSecret;
	private final String minioSecretKey;
	private final String environmentMasterKey;

	public DangerousDefaultSecretsGuard(
		Environment environment,
		@Value("${spring.datasource.password:}") String dbPassword,
		@Value("${spring.jwt.secret:}") String jwtSecret,
		@Value("${storage.minio.secret-key:}") String minioSecretKey,
		@Value("${synaipse.environment.master-key:${SYNAIPSE_ENV_MASTER_KEY:}}") String environmentMasterKey
	) {
		this.environment = environment;
		this.dbPassword = dbPassword;
		this.jwtSecret = jwtSecret;
		this.minioSecretKey = minioSecretKey;
		this.environmentMasterKey = environmentMasterKey;
	}

	@PostConstruct
	public void validate() {
		String[] activeProfiles = environment.getActiveProfiles();
		boolean runningInSafeProfile = Arrays.stream(activeProfiles).anyMatch(SAFE_PROFILES::contains);
		if (runningInSafeProfile) {
			return;
		}

		List<String> offenders = new ArrayList<>();
		if (DEFAULT_DB_PASSWORD.equals(dbPassword)) {
			offenders.add("spring.datasource.password (DB_PASSWORD)");
		}
		if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
			offenders.add("spring.jwt.secret (JWT_SECRET)");
		}
		if (DEFAULT_MINIO_SECRET_KEY.equals(minioSecretKey)) {
			offenders.add("storage.minio.secret-key (MINIO_ROOT_PASSWORD)");
		}
		if (environmentMasterKey == null || environmentMasterKey.isBlank()) {
			offenders.add("synaipse.environment.master-key (SYNAIPSE_ENV_MASTER_KEY)");
		}

		if (!offenders.isEmpty()) {
			throw new IllegalStateException(
				"Refusing to start outside the 'dev'/'test' profiles with well-known placeholder "
					+ "values still set for: " + offenders
					+ ". Set real values via environment variables before deploying. Active profiles="
					+ Arrays.toString(activeProfiles)
			);
		}
	}
}
