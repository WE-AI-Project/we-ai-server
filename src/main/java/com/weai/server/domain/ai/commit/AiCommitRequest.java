package com.weai.server.domain.ai.commit;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "RAG-based AI commit message request")
public record AiCommitRequest(
	@Schema(description = "Workspace/project id used to isolate RAG retrieval", example = "1")
	@NotNull(message = "projectId is required.")
	Long projectId,

	@Schema(description = "Git diff text to summarize into commit messages")
	@NotBlank(message = "diff is required.")
	String diff,

	@ArraySchema(schema = @Schema(description = "Changed file path", example = "src/main/java/com/weai/server/domain/ai/AiService.java"))
	List<String> files
) {
}
