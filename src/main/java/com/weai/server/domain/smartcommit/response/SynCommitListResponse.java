package com.weai.server.domain.smartcommit.response;

import com.weai.server.domain.smartcommit.domain.SynCommit;
import com.weai.server.domain.smartcommit.domain.SynCommitType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Syn commit list")
public record SynCommitListResponse(
	@Schema(description = "Project ID", example = "1")
	Long projectId,

	@Schema(description = "Total number of syn commits", example = "12")
	long totalCount,

	@Schema(description = "Current page number", example = "0")
	int page,

	@Schema(description = "Page size", example = "20")
	int size,

	@Schema(description = "Total page count", example = "1")
	int totalPages,

	@ArraySchema(schema = @Schema(implementation = SynCommitSummaryResponse.class))
	List<SynCommitSummaryResponse> commits
) {

	public static SynCommitListResponse from(Long projectId, Page<SynCommit> commitPage) {
		return new SynCommitListResponse(
			projectId,
			commitPage.getTotalElements(),
			commitPage.getNumber(),
			commitPage.getSize(),
			commitPage.getTotalPages(),
			commitPage.getContent().stream().map(SynCommitSummaryResponse::from).toList()
		);
	}

	@Schema(description = "Syn commit list item")
	public record SynCommitSummaryResponse(
		@Schema(description = "Syn commit ID", example = "1")
		Long synCommitId,

		@Schema(description = "How the syn commit was created", example = "AUTO")
		SynCommitType type,

		@Schema(description = "Semantic commit style message", example = "feat: add project dashboard summary")
		String commitMessage,

		@Schema(description = "Number of files folded into this syn commit", example = "3")
		int changedFileCount,

		@Schema(description = "When the syn commit was created")
		LocalDateTime committedAt
	) {

		public static SynCommitSummaryResponse from(SynCommit commit) {
			return new SynCommitSummaryResponse(
				commit.getId(),
				commit.getType(),
				commit.getCommitMessage(),
				commit.getChangedFileCount(),
				commit.getCommittedAt()
			);
		}
	}
}
