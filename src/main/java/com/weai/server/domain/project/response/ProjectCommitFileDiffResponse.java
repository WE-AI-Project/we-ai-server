package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectRepositoryType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 커밋 파일 Diff 응답")
public record ProjectCommitFileDiffResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "저장소 구분", example = "BACKEND")
	ProjectRepositoryType repositoryType,

	@Schema(description = "커밋 해시", example = "32a4ffcb47ed8b98168f0d117f41fcbec5c2bb08")
	String commitHash,

	@Schema(description = "파일 경로", example = "src/main/java/com/weai/server/domain/project/service/ProjectService.java")
	String filePath,

	@Schema(description = "파일명", example = "ProjectService.java")
	String fileName,

	@Schema(description = "확장자", example = "java")
	String extension,

	@Schema(description = "파일 상태", example = "MODIFIED")
	String status,

	@Schema(description = "추가된 라인 수", example = "24")
	long additions,

	@Schema(description = "삭제된 라인 수", example = "6")
	long deletions,

	@Schema(description = "Unified diff 원문")
	String diff
) {
}
