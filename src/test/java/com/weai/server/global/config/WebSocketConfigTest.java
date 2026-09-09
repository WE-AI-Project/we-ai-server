package com.weai.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebSocketConfigTest {

	@Test
	@DisplayName("기본 설정 시 localhost 및 Vercel 도메인 패턴이 포함된다")
	void defaultPatternsIncludeLocalhostAndVercel() {
		AppWebProperties properties = new AppWebProperties();
		WebSocketConfig config = new WebSocketConfig(properties, null);

		String[] patterns = config.resolveAllowedOriginPatterns();

		assertThat(patterns).contains(
			"http://localhost:*",
			"http://127.0.0.1:*",
			"https://*.vercel.app"
		);
	}

	@Test
	@DisplayName("allowedOrigins 및 allowedOriginPatterns에 지정된 외부 배포 도메인이 WebSocket 패턴에 모두 병합된다")
	void mergesAllowedOriginsAndOriginPatterns() {
		AppWebProperties properties = new AppWebProperties();
		properties.setFrontendBaseUrl("https://we-ai-client.vercel.app");
		properties.getCors().setAllowedOrigins(List.of(
			"https://we-ai-client.vercel.app",
			"https://my-custom-domain.com"
		));
		properties.getCors().setAllowedOriginPatterns(List.of(
			"https://*-company.vercel.app"
		));

		WebSocketConfig config = new WebSocketConfig(properties, null);
		String[] patterns = config.resolveAllowedOriginPatterns();

		assertThat(patterns).contains(
			"https://we-ai-client.vercel.app",
			"https://my-custom-domain.com",
			"https://*-company.vercel.app",
			"https://*.vercel.app",
			"http://localhost:*",
			"http://127.0.0.1:*"
		);
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
}
