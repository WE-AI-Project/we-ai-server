package com.weai.server.domain.project.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectStackDetectRequest(
	@NotBlank(message = "localPath is required.")
	@Size(max = 500, message = "localPath must be 500 characters or fewer.")
	String localPath
) {
}
