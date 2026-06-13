package com.weai.server.domain.ai.debate;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "VS Code editor context plus selected agents for a custom AI debate")
public record CustomDebateRequest(
	@NotNull(message = "context is required.")
	@Valid
	@Schema(description = "Editor context shared with the selected agents")
	EditorContextDto context,

	@NotEmpty(message = "agents must contain at least one agent.")
	@ArraySchema(schema = @Schema(implementation = AiAgentType.class))
	List<AiAgentType> agents,

	@Min(value = 1, message = "maxRounds must be greater than or equal to 1.")
	@Max(value = 20, message = "maxRounds must be less than or equal to 20.")
	@Schema(description = "Optional per-request round limit. Defaults to server ai.debate.max-rounds.", example = "3")
	Integer maxRounds
) {
}
