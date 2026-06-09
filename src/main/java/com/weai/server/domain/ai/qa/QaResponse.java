package com.weai.server.domain.ai.qa;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structured AI QA result for a code diff")
public record QaResponse(
	@JsonProperty("bug_report")
	@Schema(description = "Detected bug risk or functional concern", example = "Returning null can move the failure point and cause a NullPointerException later in the flow.")
	String bugReport,

	@JsonProperty("optimization")
	@Schema(description = "Optimization or maintainability recommendation", example = "Throw a domain-specific exception with context instead of returning null.")
	String optimization,

	@JsonProperty("commit_msg")
	@Schema(description = "Recommended semantic commit message", example = "fix: replace nullable user lookup with explicit not-found handling")
	String commitMsg
) {
}
