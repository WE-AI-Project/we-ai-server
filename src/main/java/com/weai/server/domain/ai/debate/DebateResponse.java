package com.weai.server.domain.ai.debate;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Multi-agent debate result")
public record DebateResponse(
	@Schema(description = "Original question passed to the debate chain", example = "How should we structure a project schedule API?")
	String query,

	@Schema(description = "Oracle opening statement that frames the problem", example = "Let me restate the scope, constraints, and success criteria.")
	String oracleOpening,

	@Schema(description = "Architect analysis focused on backend architecture and data design", example = "Separating schedule, assignee, and status history will keep the design safer.")
	String architectOpinion,

	@Schema(description = "Sync review focused on QA, bug risk, and planning impact", example = "Status transition rules and filter combinations need strong test coverage.")
	String syncOpinion,

	@Schema(description = "Oracle final conclusion after reviewing the whole conversation", example = "Use a single schedule aggregate with explicit state rules and a dedicated filter endpoint.")
	String finalConclusion,

	@ArraySchema(schema = @Schema(implementation = DebateTurn.class))
	List<DebateTurn> debateHistory
) {

	@Schema(description = "One agent turn in the debate")
	public record DebateTurn(
		@Schema(description = "Agent name", example = "Oracle")
		String agent,

		@Schema(description = "Agent persona", example = "PM")
		String role,

		@Schema(description = "Generated message", example = "I will first clarify the scope and success criteria.")
		String message
	) {
	}
}
