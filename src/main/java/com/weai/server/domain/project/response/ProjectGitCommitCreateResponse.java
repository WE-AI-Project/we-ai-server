package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Git 커밋 생성 응답")
public record ProjectGitCommitCreateResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "커밋 해시", example = "a1b2c3d4e5f6")
	String commitHash,

	@Schema(description = "짧은 커밋 해시", example = "a1b2c3d")
	String shortCommitHash,

	@Schema(description = "현재 브랜치명", example = "feature/Synaipse-10-7-commit-create")
	String branchName,

	@Schema(description = "커밋 제목 메시지", example = "feat: 변경 파일 목록 조회 API 구현")
	String message,

	@Schema(description = "커밋된 파일 수", example = "2")
	int committedFileCount,

	@ArraySchema(schema = @Schema(description = "커밋된 파일 경로", example = "src/main/java/com/example/project/ChangesService.java"))
	List<String> committedFiles,

	@Schema(description = "커밋 생성 시각", example = "2026-09-05T16:20:00")
	LocalDateTime createdAt
) {
}
