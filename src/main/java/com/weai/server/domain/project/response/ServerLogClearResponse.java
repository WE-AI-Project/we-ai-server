package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "서버 로그 초기화 결과")
public record ServerLogClearResponse(
	@Schema(description = "프로젝트 ID", example = "1") Long projectId,
	@Schema(description = "초기화된 로그 개수", example = "127") int clearedCount,
	@Schema(description = "초기화 시각") LocalDateTime clearedAt
) {
}
