package com.weai.server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Builder 없이 직접 팩토리를 생성하여 타임아웃 설정 (밀리초 단위)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5초 (5000ms)
        factory.setReadTimeout(5000);    // 5초 (5000ms)

        return new RestTemplate(factory);
    }
}