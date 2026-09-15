package com.weai.server.domain.smartcommit.response;

import com.weai.server.domain.smartcommit.domain.SynCommit;
import com.weai.server.domain.smartcommit.domain.SynCommitFile;
import com.weai.server.domain.smartcommit.domain.SynCommitType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Syn commit detail")
public record SynCommitResponse(
	@Schema(description = "Syn commit ID", example = "1")
	Long synCommitId,

	@Schema(description = "How the syn commit was created", example = "AUTO")
	SynCommitType type,

	@Schema(description = "Semantic commit style message", example = "feat: add project dashboard summary")
	String commitMessage,

	@Schema(description = "Full AI-generated summary of the change")
	String summary,

	@Schema(description = "Number of files folded into this syn commit", example = "3")
	int changedFileCount,

	@Schema(description = "When the syn commit was created")
	LocalDateTime committedAt,

	@ArraySchema(schema = @Schema(implementation = SynCommitFileResponse.class))
	List<SynCommitFileResponse> files
) {

	public static SynCommitResponse from(SynCommit commit, List<SynCommitFile> files) {
		return new SynCommitResponse(
			commit.getId(),
			commit.getType(),
			commit.getCommitMessage(),
			commit.getSummary(),
			commit.getChangedFileCount(),
			commit.getCommittedAt(),
			files.stream().map(SynCommitFileResponse::from).toList()
		);
	}

	@Schema(description = "One file's diff folded into a syn commit")
	public record SynCommitFileResponse(
		@Schema(description = "File path", example = "src/main/java/com/example/Foo.java")
		String filePath,

		@Schema(description = "Diff captured on save")
		String diffContent
	) {

		public static SynCommitFileResponse from(SynCommitFile file) {
			return new SynCommitFileResponse(file.getFilePath(), file.getDiffContent());
		}
	}
}
