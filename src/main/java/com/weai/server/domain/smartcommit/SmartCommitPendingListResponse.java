package com.weai.server.domain.smartcommit;

import com.weai.server.domain.smartcommit.domain.SynPendingChange;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Currently staged (\"syn add\") pending changes for a project")
public record SmartCommitPendingListResponse(
	@Schema(description = "Workspace/project id", example = "1")
	Long projectId,

	@ArraySchema(schema = @Schema(implementation = PendingChangeItem.class))
	List<PendingChangeItem> files
) {

	public static SmartCommitPendingListResponse from(Long projectId, List<SynPendingChange> changes) {
		return new SmartCommitPendingListResponse(
			projectId,
			changes.stream().map(PendingChangeItem::from).toList()
		);
	}

	@Schema(description = "One staged file's diff, waiting to be folded into a syn commit")
	public record PendingChangeItem(
		@Schema(description = "File path relative to the project root", example = "src/main/java/com/example/UserService.java")
		String filePath,

		@Schema(description = "Diff captured by \"syn add\"")
		String diffContent,

		@Schema(description = "When this file's diff was last staged/updated")
		LocalDateTime updatedAt
	) {

		private static PendingChangeItem from(SynPendingChange change) {
			return new PendingChangeItem(change.getFilePath(), change.getDiffContent(), change.getUpdatedAt());
		}
	}
}
