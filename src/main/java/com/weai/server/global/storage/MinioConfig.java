package com.weai.server.global.storage;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class MinioConfig {

	private final StorageProperties storageProperties;

	@Bean
	@ConditionalOnProperty(name = "storage.minio.enabled", havingValue = "true", matchIfMissing = true)
	public MinioClient minioClient() {
		return MinioClient.builder()
			.endpoint(storageProperties.getEndpoint())
			.credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey())
			.build();
	}
}
