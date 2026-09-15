package com.weai.server.global.config;

import com.weai.server.global.security.jwt.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final AppWebProperties appWebProperties;
	private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		String[] allowedPatterns = resolveAllowedOriginPatterns();

		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns(allowedPatterns);

		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns(allowedPatterns)
			.withSockJS();
	}

	public String[] resolveAllowedOriginPatterns() {
		java.util.Set<String> patterns = new java.util.LinkedHashSet<>();
		AppWebProperties.Cors cors = appWebProperties.getCors();

		if (cors != null) {
			java.util.List<String> origins = cors.getAllowedOrigins();
			if (origins != null) {
				for (String origin : origins) {
					if (org.springframework.util.StringUtils.hasText(origin)) {
						patterns.add(origin.trim());
					}
				}
			}

			java.util.List<String> originPatterns = cors.getAllowedOriginPatterns();
			if (originPatterns != null) {
				for (String pattern : originPatterns) {
					if (org.springframework.util.StringUtils.hasText(pattern)) {
						patterns.add(pattern.trim());
					}
				}
			}
		}

		if (org.springframework.util.StringUtils.hasText(appWebProperties.getFrontendBaseUrl())) {
			patterns.add(appWebProperties.getFrontendBaseUrl().trim());
		}

		// WebSocket 허용 origin은 app.web.cors.allowed-origins / allowed-origin-patterns 설정을
		// 그대로 따른다. 예전에는 여기서 http://localhost:*, https://*.vercel.app 를 설정과
		// 무관하게 무조건 추가했는데, 이러면 prod에서 해당 패턴을 지우거나 좁혀도 항상 다시
		// 열리는 문제가 있었다 — 필요하면 각 프로필의 yml에서 명시적으로 켜야 한다.

		return patterns.toArray(new String[0]);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthChannelInterceptor);
	}
}
