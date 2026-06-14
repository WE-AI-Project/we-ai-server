package com.weai.server.domain.ai.commit;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "RAG-based AI commit message request")
public record AiCommitRequest(
	@Schema(description = "Workspace/project id used to isolate RAG retrieval", example = "1")
	@NotNull(message = "projectId is required.")
	Long projectId,

	@Schema(description = "Git diff text to summarize into commit messages")
	@NotBlank(message = "diff is required.")
	@Size(max = 200000, message = "diff must be 200000 characters or fewer.")
	String diff,

	@ArraySchema(schema = @Schema(description = "Changed file path", example = "src/main/java/com/weai/server/domain/ai/AiService.java"))
	@Size(max = 500, message = "files must contain 500 entries or fewer.")
	List<@Size(max = 1000, message = "file path must be 1000 characters or fewer.") String> files
) {
}
