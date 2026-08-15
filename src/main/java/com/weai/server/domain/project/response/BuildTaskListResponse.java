package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "빌드 태스크 목록 조회 응답")
public record BuildTaskListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "빌드 도구", example = "GRADLE")
	String buildTool,

	@ArraySchema(schema = @Schema(implementation = BuildTaskResponse.class))
	List<BuildTaskResponse> tasks
) {

	@Schema(description = "빌드 태스크")
	public record BuildTaskResponse(
		@Schema(description = "태스크 이름", example = "bootRun")
		String taskName,

		@Schema(description = "표시 이름", example = "Spring Boot 실행")
		String displayName,

		@Schema(description = "설명", example = "Spring Boot 애플리케이션을 실행합니다.")
		String description,

		@Schema(description = "실행 명령", example = "./gradlew bootRun")
		String command,

		@Schema(description = "카테고리", example = "RUN")
		String category,

		@Schema(description = "주의가 필요한 태스크 여부", example = "false")
		boolean dangerous,

		@Schema(description = "사용 가능 여부", example = "true")
		boolean enabled
	) {
	}
}
