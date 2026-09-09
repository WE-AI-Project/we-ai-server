package com.weai.server.domain.ai.debate;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgentInvocationLogRepository extends JpaRepository<AgentInvocationLog, Long> {

	long countByAgent(AiAgentType agent);

	long countByAgentAndSuccess(AiAgentType agent, boolean success);

	Optional<AgentInvocationLog> findFirstByAgentOrderByCreatedAtDesc(AiAgentType agent);

	List<AgentInvocationLog> findByAgentOrderByCreatedAtDesc(AiAgentType agent, Pageable pageable);

	@Query("select avg(log.durationMs) from AgentInvocationLog log where log.agent = :agent")
	Double findAverageDurationMs(@Param("agent") AiAgentType agent);
}
