package com.weai.server.domain.ai.debate;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "에이전트별 실제 호출 성능/가동 현황 (프로세스 CPU/메모리는 이 애플리케이션에 존재하지 않아 집계하지 않음)")
public record AgentMetricsResponse(
	@Schema(description = "에이전트 키", example = "ORACLE")
	AiAgentType agent,

	@Schema(description = "표시 이름", example = "Oracle")
	String displayName,

	@Schema(description = "역할", example = "Chief coordinator")
	String role,

	@Schema(description = "사용 모델", example = "llama3.1")
	String model,

	@Schema(description = "총 호출 횟수", example = "128")
	long totalInvocations,

	@Schema(description = "성공 횟수", example = "120")
	long successCount,

	@Schema(description = "실패 횟수", example = "8")
	long failureCount,

	@Schema(description = "평균 응답 시간(ms)", example = "3400")
	long avgDurationMs,

	@Schema(description = "마지막 호출 일시 (호출 이력이 없으면 null)")
	LocalDateTime lastInvokedAt
) {
}
