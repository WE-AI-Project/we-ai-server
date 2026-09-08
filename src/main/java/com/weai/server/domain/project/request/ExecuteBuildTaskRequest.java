package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "빌드 태스크 실행 요청")
public record ExecuteBuildTaskRequest(
	@Schema(description = "실행할 빌드 태스크 이름 (예: build, test, clean, bootJar, dependencies, check)", example = "build")
	@NotBlank(message = "태스크 이름은 필수입니다.")
	String taskName
) {
}
