package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Spring Profile 목록 조회 응답")
public record SpringProfileListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "프로젝트 설정에 저장된 Active profile", example = "dev")
	String activeProfile,

	@ArraySchema(schema = @Schema(implementation = SpringProfileResponse.class))
	List<SpringProfileResponse> profiles
) {

	@Schema(description = "Spring Profile 정보")
	public record SpringProfileResponse(
		@Schema(description = "Spring profile", example = "dev")
		String profile,

		@Schema(description = "표시 이름", example = "Development")
		String displayName,

		@Schema(description = "설명", example = "개발 서버 환경")
		String description,

		@Schema(description = "Active profile 여부", example = "true")
		boolean active,

		@Schema(description = "수정 가능 여부", example = "false")
		boolean editable
	) {
	}
}
