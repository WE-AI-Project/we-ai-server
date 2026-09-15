package com.weai.server.domain.auth.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start the application if the email/Kakao verification-code mock flags
 * ({@code auth.verification.mock-enabled}, {@code auth.verification.expose-code-in-response}) are
 * turned on outside the {@code dev}/{@code test} profiles.
 *
 * These flags skip sending the code and/or return it directly in the API response, which is a
 * full authentication bypass if it ever reaches a real deployment. A misconfigured {@code .env}
 * (e.g. {@code APP_PROFILE=prod} with {@code AUTH_VERIFICATION_MOCK_ENABLED=true}) must fail
 * loudly at boot rather than silently ship the bypass.
 */
@Component
@RequiredArgsConstructor
public class AuthVerificationSecurityGuard {

	private static final Set<String> SAFE_PROFILES = Set.of("dev", "test");

	private final AuthVerificationProperties authVerificationProperties;
	private final Environment environment;

	@PostConstruct
	public void validate() {
		String[] activeProfiles = environment.getActiveProfiles();
		boolean runningInSafeProfile = Arrays.stream(activeProfiles).anyMatch(SAFE_PROFILES::contains);
		if (runningInSafeProfile) {
			return;
		}

		if (authVerificationProperties.isMockEnabled() || authVerificationProperties.isExposeCodeInResponse()) {
			throw new IllegalStateException(
				"auth.verification.mock-enabled and auth.verification.expose-code-in-response must stay false "
					+ "outside the 'dev'/'test' profiles. Refusing to start with active profiles="
					+ Arrays.toString(activeProfiles)
					+ " to avoid shipping an authentication bypass to a real deployment."
			);
		}
	}
}
