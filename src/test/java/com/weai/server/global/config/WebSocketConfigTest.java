package com.weai.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebSocketConfigTest {

	@Test
	@DisplayName("기본(Java) 설정 시 AppWebProperties.Cors의 기본 패턴만 포함되고, 그 이상은 임의로 추가되지 않는다")
	void defaultPatternsComeOnlyFromConfiguredCorsDefaults() {
		AppWebProperties properties = new AppWebProperties();
		WebSocketConfig config = new WebSocketConfig(properties, null);

		String[] patterns = config.resolveAllowedOriginPatterns();

		// AppWebProperties.Cors 기본값(allowedOrigins + allowedOriginPatterns)과 frontendBaseUrl
		// 에서 나온 값들뿐이어야 한다 — yml에서 얼마든지 좁힐 수 있어야 한다.
		java.util.Set<String> expected = new java.util.LinkedHashSet<>(properties.getCors().getAllowedOrigins());
		expected.addAll(properties.getCors().getAllowedOriginPatterns());
		expected.add(properties.getFrontendBaseUrl());
		assertThat(patterns).containsExactlyInAnyOrderElementsOf(expected);
	}

	@Test
	@DisplayName("allowedOriginPatterns를 좁게 설정하면 WebSocket도 그 설정만 따르고, 하드코딩된 와일드카드는 더 이상 강제로 추가되지 않는다")
	void doesNotForceAddHardcodedWildcardsBeyondConfiguredPatterns() {
		AppWebProperties properties = new AppWebProperties();
		properties.setFrontendBaseUrl("https://we-ai-client.vercel.app");
		properties.getCors().setAllowedOrigins(List.of(
			"https://we-ai-client.vercel.app",
			"https://my-custom-domain.com"
		));
		// 운영자가 의도적으로 localhost/*.vercel.app 와일드카드를 빼고, 회사 전용 패턴만 남긴 경우.
		properties.getCors().setAllowedOriginPatterns(List.of(
			"https://*-company.vercel.app"
		));

		WebSocketConfig config = new WebSocketConfig(properties, null);
		String[] patterns = config.resolveAllowedOriginPatterns();

		assertThat(patterns).containsExactlyInAnyOrder(
			"https://we-ai-client.vercel.app",
			"https://my-custom-domain.com",
			"https://*-company.vercel.app"
		);
		assertThat(patterns).doesNotContain("http://localhost:*", "http://127.0.0.1:*", "https://*.vercel.app");
	}

	@Test
	@DisplayName("공백이나 null 값이 있어도 안전하게 처리되며 중복이 제거된다")
	void handlesNullAndBlankSafelyWithoutDuplicates() {
		AppWebProperties properties = new AppWebProperties();
		properties.getCors().setAllowedOrigins(List.of("  ", "https://we-ai-client.vercel.app"));
		properties.getCors().setAllowedOriginPatterns(List.of("https://we-ai-client.vercel.app", ""));

		WebSocketConfig config = new WebSocketConfig(properties, null);
		String[] patterns = config.resolveAllowedOriginPatterns();

		assertThat(patterns).contains("https://we-ai-client.vercel.app");
		assertThat(patterns).doesNotContain("", "  ");

		long count = java.util.Arrays.stream(patterns)
			.filter("https://we-ai-client.vercel.app"::equals)
			.count();
		assertThat(count).isEqualTo(1);
	}

	@Test
	@DisplayName("prod/stag처럼 패턴 env var가 전부 미설정(빈 문자열)이면 와일드카드 패턴이 하나도 남지 않는다")
	void prodStyleBlankPatternEnvVarsLeaveNoWildcards() {
		AppWebProperties properties = new AppWebProperties();
		properties.setFrontendBaseUrl("https://we-ai-client.vercel.app");
		properties.getCors().setAllowedOrigins(List.of("https://we-ai-client.vercel.app"));
		// application-prod.yml / application-stag.yml 이 실제로 이렇게 생겼다:
		// ${APP_LOCALHOST_FRONTEND_PATTERN:}, ${APP_LOCALHOST_LOOPBACK_PATTERN:}, ${APP_VERCEL_FRONTEND_PATTERN:}
		properties.getCors().setAllowedOriginPatterns(List.of("", "", ""));

		WebSocketConfig config = new WebSocketConfig(properties, null);
		String[] patterns = config.resolveAllowedOriginPatterns();

		assertThat(patterns).containsExactly("https://we-ai-client.vercel.app");
	}
}
