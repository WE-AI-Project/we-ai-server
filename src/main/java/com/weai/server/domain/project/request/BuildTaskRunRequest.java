package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "빌드 태스크 비동기 실행 요청")
public record BuildTaskRunRequest(
	@Schema(description = "실행할 Gradle 태스크 이름", example = "build")
	String taskName,

	@Schema(description = "Spring profile", example = "dev", nullable = true)
	String profile
) {
}
