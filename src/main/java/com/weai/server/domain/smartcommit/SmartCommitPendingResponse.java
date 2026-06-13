package com.weai.server.domain.smartcommit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Pending smart-commit diff registration result")
public record SmartCommitPendingResponse(
	@Schema(description = "Number of files currently waiting for smart commit", example = "3")
	int pendingFileCount,

	@Schema(description = "Last pending diff update timestamp")
	Instant lastModifiedTime
) {
}
