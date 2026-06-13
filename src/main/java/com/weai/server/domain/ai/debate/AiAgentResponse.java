package com.weai.server.domain.ai.debate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Selectable AI agent metadata")
public record AiAgentResponse(
	@Schema(description = "Agent key used in requests", example = "BACKEND")
	AiAgentType agent,

	@Schema(description = "Human-readable agent name", example = "Backend")
	String name,

	@Schema(description = "Agent role", example = "Server/API specialist")
	String role,

	@Schema(description = "Model backing this agent", example = "qwen2.5-coder")
	String model
) {
	public static AiAgentResponse from(AiAgentType agent) {
		return new AiAgentResponse(agent, agent.displayName(), agent.role(), agent.model());
	}
}
