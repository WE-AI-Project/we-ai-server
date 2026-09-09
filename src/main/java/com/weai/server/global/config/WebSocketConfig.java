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

		// 기본 패턴 보장: 로컬 개발 환경 및 Vercel 배포 도메인
		patterns.add("http://localhost:*");
		patterns.add("http://127.0.0.1:*");
		patterns.add("https://*.vercel.app");

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
