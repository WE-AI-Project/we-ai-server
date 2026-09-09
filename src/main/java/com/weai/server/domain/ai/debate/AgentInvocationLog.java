package com.weai.server.domain.ai.debate;

import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실제 에이전트(ORACLE/BACKEND/FRONTEND/INSPECTOR) 호출 1건을 기록한다.
 * CPU/메모리 같은 프로세스 자원 지표는 이 애플리케이션에 존재하지 않으므로(에이전트는
 * OS 프로세스가 아니라 LLM 호출 단위), 실제로 측정 가능한 "호출 성능/가동 현황"을 기록한다.
 */
@Getter
@Entity
@Table(
	name = "agent_invocation_logs",
	indexes = {
		@Index(name = "idx_agent_invocation_logs_agent_created_at", columnList = "agent, created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AgentInvocationLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AiAgentType agent;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(nullable = false)
	private boolean success;

	@Column(name = "duration_ms", nullable = false)
	private long durationMs;

	@Column(name = "error_message", length = 2000)
	private String errorMessage;

	public static AgentInvocationLog of(AiAgentType agent, Long projectId, boolean success, long durationMs, String errorMessage) {
		return AgentInvocationLog.builder()
			.agent(agent)
			.projectId(projectId)
			.success(success)
			.durationMs(durationMs)
			.errorMessage(errorMessage)
			.build();
	}
}
