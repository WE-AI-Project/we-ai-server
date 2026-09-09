package com.weai.server.domain.ai.debate;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentMetricsService {

	private final AgentInvocationLogRepository agentInvocationLogRepository;

	@Transactional
	public void record(AiAgentType agent, Long projectId, boolean success, long durationMs, String errorMessage) {
		agentInvocationLogRepository.save(
			AgentInvocationLog.of(agent, projectId, success, durationMs, errorMessage)
		);
	}

	@Transactional(readOnly = true)
	public List<AgentInvocationResponse> getRecentInvocations(AiAgentType agent, int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 100));
		return agentInvocationLogRepository
			.findByAgentOrderByCreatedAtDesc(agent, PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt")))
			.stream()
			.map(AgentInvocationResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<AgentMetricsResponse> getMetrics() {
		return Arrays.stream(AiAgentType.values())
			.map(this::buildMetrics)
			.toList();
	}

	private AgentMetricsResponse buildMetrics(AiAgentType agent) {
		long total = agentInvocationLogRepository.countByAgent(agent);
		long successCount = agentInvocationLogRepository.countByAgentAndSuccess(agent, true);
		Double avgDurationMs = agentInvocationLogRepository.findAverageDurationMs(agent);

		return new AgentMetricsResponse(
			agent,
			agent.displayName(),
			agent.role(),
			agent.model(),
			total,
			successCount,
			total - successCount,
			avgDurationMs == null ? 0L : Math.round(avgDurationMs),
			agentInvocationLogRepository.findFirstByAgentOrderByCreatedAtDesc(agent)
				.map(AgentInvocationLog::getCreatedAt)
				.orElse(null)
		);
	}
}
