package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectRepositoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "프로젝트 커밋 상세 응답")
public record ProjectCommitDetailResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "저장소 구분", example = "BACKEND")
	ProjectRepositoryType repositoryType,

	@Schema(description = "커밋 해시", example = "32a4ffcb47ed8b98168f0d117f41fcbec5c2bb08")
	String commitHash,

	@Schema(description = "짧은 커밋 해시", example = "32a4ffc")
	String shortCommitHash,

	@Schema(description = "커밋 제목", example = "feat(project): add dashboard detail endpoints")
	String message,

	@Schema(description = "커밋 본문", example = "Connect dashboard tabs to dedicated APIs.")
	String body,

	@Schema(description = "작성자 이름", example = "Roy Kim")
	String authorName,

	@Schema(description = "작성자 이메일", example = "royalkim@example.com")
	String authorEmail,

	@Schema(description = "커밋 시각", example = "2026-06-15T10:30:00")
	LocalDateTime committedAt,

	@Schema(description = "변경 파일 수", example = "4")
	long changedFileCount,

	@Schema(description = "추가된 라인 수", example = "128")
	long additions,

	@Schema(description = "삭제된 라인 수", example = "32")
	long deletions
) {
}
