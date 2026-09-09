package com.weai.server.domain.ai.debate;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "에이전트 호출 이력 1건")
public record AgentInvocationResponse(
	@Schema(description = "호출된 프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "성공 여부", example = "true")
	boolean success,

	@Schema(description = "응답 시간(ms)", example = "3400")
	long durationMs,

	@Schema(description = "실패 시 에러 메시지 (성공이면 null)")
	String errorMessage,

	@Schema(description = "호출 일시")
	LocalDateTime createdAt
) {
	public static AgentInvocationResponse from(AgentInvocationLog log) {
		return new AgentInvocationResponse(
			log.getProjectId(),
			log.isSuccess(),
			log.getDurationMs(),
			log.getErrorMessage(),
			log.getCreatedAt()
		);
	}
}
