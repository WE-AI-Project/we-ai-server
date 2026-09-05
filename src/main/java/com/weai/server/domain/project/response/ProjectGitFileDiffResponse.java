package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Git 변경 파일 Diff 응답")
public record ProjectGitFileDiffResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "파일 경로", example = "src/main/java/com/example/project/ProjectService.java")
	String filePath,

	@Schema(description = "파일명", example = "ProjectService.java")
	String fileName,

	@Schema(description = "확장자", example = "java")
	String extension,

	@Schema(description = "staged diff 조회 여부", example = "false")
	boolean staged,

	@Schema(description = "변경 유형", example = "MODIFIED")
	String changeType,

	@Schema(description = "추가된 라인 수", example = "12")
	long additions,

	@Schema(description = "삭제된 라인 수", example = "3")
	long deletions,

	@Schema(description = "Unified diff 원문")
	String diffContent
) {
}
