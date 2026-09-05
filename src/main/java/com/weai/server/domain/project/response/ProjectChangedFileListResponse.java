package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Git 변경 파일 목록 응답")
public record ProjectChangedFileListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "현재 브랜치명", example = "feature/Synaipse-10-1-changes-files")
	String branchName,

	@Schema(description = "전체 변경 파일 수", example = "3")
	int totalChangedCount,

	@Schema(description = "staged 파일 수", example = "1")
	int stagedCount,

	@Schema(description = "unstaged 파일 수", example = "1")
	int unstagedCount,

	@Schema(description = "untracked 파일 수", example = "1")
	int untrackedCount,

	@ArraySchema(schema = @Schema(implementation = ChangedFileResponse.class))
	List<ChangedFileResponse> files
) {

	@Schema(description = "Git 변경 파일 정보")
	public record ChangedFileResponse(
		@Schema(description = "파일 경로", example = "src/main/java/com/example/project/ProjectService.java")
		String filePath,

		@Schema(description = "파일명", example = "ProjectService.java")
		String fileName,

		@Schema(description = "확장자", example = "java")
		String extension,

		@Schema(description = "변경 유형", example = "MODIFIED")
		String changeType,

		@Schema(description = "staging area 포함 여부", example = "true")
		boolean staged,

		@Schema(description = "working tree 변경 여부", example = "false")
		boolean unstaged,

		@Schema(description = "git status 첫 번째 문자", example = "M")
		String stagedStatus,

		@Schema(description = "git status 두 번째 문자", example = "M")
		String unstagedStatus,

		@Schema(description = "화면 표시 상태", example = "STAGED_MODIFIED")
		String displayStatus
	) {
	}
}
