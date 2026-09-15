package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Active Spring Profile 변경 요청")
public record ActiveSpringProfileUpdateRequest(
	@Schema(description = "변경할 Spring profile", example = "prod")
	String profile
) {
}
