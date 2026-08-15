package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "프로파일별 실행 명령 조회 응답")
public record ProfileRunCommandListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "빌드 도구", example = "GRADLE")
	String buildTool,

	@Schema(description = "운영체제 유형", example = "WINDOWS")
	String osType,

	@ArraySchema(schema = @Schema(implementation = ProfileRunCommandResponse.class))
	List<ProfileRunCommandResponse> commands
) {

	@Schema(description = "프로파일 실행 명령")
	public record ProfileRunCommandResponse(
		@Schema(description = "Spring profile", example = "dev")
		String profile,

		@Schema(description = "설명", example = "개발 환경 실행 명령입니다.")
		String description,

		@Schema(description = "Windows 실행 명령", example = "$env:SPRING_PROFILES_ACTIVE=\"dev\"; ./gradlew.bat bootRun")
		String windowsCommand,

		@Schema(description = "Unix/Mac 실행 명령", example = "SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun")
		String unixCommand,

		@Schema(description = "현재 필터 프로파일 여부", example = "true")
		boolean active
	) {
	}
}
