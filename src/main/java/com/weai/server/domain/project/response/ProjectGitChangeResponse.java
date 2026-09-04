package com.weai.server.domain.project.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Git 변경사항 스테이징 처리 응답")
public record ProjectGitChangeResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "현재 staged 파일 수", example = "2")
	Integer stagedFileCount,

	@Schema(description = "현재 unstaged 파일 수", example = "3")
	Integer unstagedFileCount,

	@Schema(description = "전체 파일 스테이징 여부", example = "true")
	Boolean stagedAll,

	@Schema(description = "전체 파일 언스테이징 여부", example = "true")
	Boolean unstagedAll,

	@ArraySchema(schema = @Schema(description = "요청 처리된 파일 경로", example = "src/main/resources/application.yml"))
	List<String> filePaths,

	@ArraySchema(schema = @Schema(implementation = GitChangeFileResponse.class))
	List<GitChangeFileResponse> stagedFiles,

	@ArraySchema(schema = @Schema(implementation = GitChangeFileResponse.class))
	List<GitChangeFileResponse> unstagedFiles
) {

	public static ProjectGitChangeResponse files(Long projectId, List<String> filePaths, GitChangeStatus status) {
		return new ProjectGitChangeResponse(
			projectId,
			status.stagedFiles().size(),
			status.unstagedFiles().size(),
			null,
			null,
			filePaths,
			status.stagedFiles(),
			status.unstagedFiles()
		);
	}

	public static ProjectGitChangeResponse stageAll(Long projectId, GitChangeStatus status) {
		return new ProjectGitChangeResponse(
			projectId,
			status.stagedFiles().size(),
			status.unstagedFiles().size(),
			true,
			null,
			null,
			status.stagedFiles(),
			status.unstagedFiles()
		);
	}

	public static ProjectGitChangeResponse unstageAll(Long projectId, GitChangeStatus status) {
		return new ProjectGitChangeResponse(
			projectId,
			status.stagedFiles().size(),
			status.unstagedFiles().size(),
			null,
			true,
			null,
			status.stagedFiles(),
			status.unstagedFiles()
		);
	}

	@Schema(description = "Git 변경 파일 정보")
	public record GitChangeFileResponse(
		@Schema(description = "파일 경로", example = "src/main/resources/application.yml")
		String path,

		@Schema(description = "파일 상태", example = "MODIFIED")
		String status,

		@Schema(description = "staging area 포함 여부", example = "true")
		boolean staged,

		@Schema(description = "working tree 변경 여부", example = "false")
		boolean unstaged
	) {
	}

	public record GitChangeStatus(
		List<GitChangeFileResponse> stagedFiles,
		List<GitChangeFileResponse> unstagedFiles
	) {
	}
}
