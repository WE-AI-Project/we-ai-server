package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.ProjectRepositoryType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "프로젝트 커밋 변경 파일 목록 응답")
public record ProjectCommitFileListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "저장소 구분", example = "FRONTEND")
	ProjectRepositoryType repositoryType,

	@Schema(description = "커밋 해시", example = "32a4ffcb47ed8b98168f0d117f41fcbec5c2bb08")
	String commitHash,

	@ArraySchema(schema = @Schema(implementation = CommitFileResponse.class))
	List<CommitFileResponse> files
) {

	@Schema(description = "커밋 변경 파일 정보")
	public record CommitFileResponse(
		@Schema(description = "파일 경로", example = "src/app/components/CommitDiffPage.tsx")
		String path,

		@Schema(description = "파일명", example = "CommitDiffPage.tsx")
		String fileName,

		@Schema(description = "확장자", example = "tsx")
		String extension,

		@Schema(description = "파일 상태", example = "MODIFIED")
		String status,

		@Schema(description = "추가된 라인 수", example = "82")
		long additions,

		@Schema(description = "삭제된 라인 수", example = "10")
		long deletions
	) {
	}
}
