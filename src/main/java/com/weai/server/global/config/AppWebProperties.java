package com.weai.server.global.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.web")
public class AppWebProperties {

	@NotBlank
	private String baseUrl = "http://localhost:8080";

	@NotBlank
	private String localBaseUrl = "http://localhost:8080";

	@NotBlank
	private String frontendBaseUrl = "http://localhost:3000";

	@Valid
	private final Cors cors = new Cors();

	@Valid
	private final ExternalDomains externalDomains = new ExternalDomains();

	@Getter
	@Setter
	public static class Cors {

		@NotEmpty
		private List<@NotBlank String> allowedOrigins = new ArrayList<>(List.of(
			"http://localhost:3000",
			"http://127.0.0.1:3000"
		));

		// 개별 원소에 @NotBlank를 걸지 않는다: prod/stag의 allowed-origin-patterns 항목들은
		// env var가 안 설정되면 빈 문자열로 resolve되도록 일부러 설계했고(패턴을 opt-in으로 만들기
		// 위함), WebSocketConfig/SecurityConfig 쪽에서 소비할 때 빈 값을 걸러낸다.
		private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
			"http://localhost:*",
			"http://127.0.0.1:*",
			"https://*.vercel.app"
		));

		@NotEmpty
		private List<@NotBlank String> allowedMethods = new ArrayList<>(List.of(
			"GET",
			"POST",
			"PUT",
			"PATCH",
			"DELETE",
			"OPTIONS"
		));

		@NotEmpty
		private List<@NotBlank String> allowedHeaders = new ArrayList<>(List.of(
			"Authorization",
			"Content-Type",
			"X-Requested-With",
			"X-Request-Id"
		));

		private List<@NotBlank String> exposedHeaders = new ArrayList<>(List.of("X-Request-Id"));

		private boolean allowCredentials = true;

		@NotNull
		private Duration maxAge = Duration.ofHours(1);
	}

	@Getter
	@Setter
	public static class ExternalDomains {

		@NotBlank
		private String apiDomain = "localhost";

		@NotBlank
		private String frontendDomain = "localhost";

		@NotBlank
		private String storageConsoleDomain = "localhost";
	}
}
