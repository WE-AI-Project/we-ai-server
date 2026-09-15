package com.weai.server.global.storage;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "storage.minio")
public class StorageProperties {

	@NotBlank
	private String endpoint = "http://localhost:9000";

	@NotBlank
	private String accessKey = "minioadmin";

	@NotBlank
	private String secretKey = "change-me-minio-password";

	@NotBlank
	private String publicBucket = "we-ai-public";

	@NotBlank
	private String privateBucket = "we-ai-private";
}
