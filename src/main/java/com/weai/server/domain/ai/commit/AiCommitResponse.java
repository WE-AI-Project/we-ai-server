package com.weai.server.domain.ai.commit;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "RAG-based AI commit message candidates")
public record AiCommitResponse(
	@Schema(description = "Primary commit message", example = "fix(ai): enforce RAG context for QA analysis")
	String message,

	@JsonProperty("commit_msg")
	@Schema(description = "Primary commit message alias for clients that use snake_case")
	String commitMsg,

	@ArraySchema(schema = @Schema(description = "Commit message candidate"))
	List<Candidate> candidates
) {

	public record Candidate(
		@Schema(description = "Full commit message including optional body")
		String message,

		@JsonProperty("commit_msg")
		@Schema(description = "Full commit message alias for clients that use snake_case")
		String commitMsg,

		@Schema(description = "Conventional commit type", example = "fix")
		String type,

		@Schema(description = "Optional conventional commit scope", example = "ai")
		String scope,

		@Schema(description = "Commit subject without type/scope prefix")
		String title,

		@Schema(description = "Optional commit body")
		String body
	) {
	}
}
