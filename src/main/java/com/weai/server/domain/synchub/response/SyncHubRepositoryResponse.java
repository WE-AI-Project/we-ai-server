package com.weai.server.domain.synchub.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "SyncHub 저장소 정보 응답")
public record SyncHubRepositoryResponse(
	@Schema(description = "프로젝트 ID", example = "22")
	Long projectId,

	@Schema(description = "git remote로 등록할 SyncHub 저장소 URL", example = "https://api.yhy-server.com/api/synchub/22.git")
	String remoteUrl,

	@Schema(description = "앱이 등록하는 remote 이름", example = "synchub")
	String remoteName,

	@Schema(description = "서버에 저장소가 생성되었는지 여부 (첫 clone/push 시 생성)", example = "true")
	boolean initialized,

	@Schema(description = "커밋이 하나도 없는지 여부", example = "false")
	boolean empty,

	@Schema(description = "기본 브랜치명", example = "main")
	String defaultBranch,

	@Schema(description = "기본 브랜치 최신 커밋 해시", example = "32a4ffcb47ed8b98168f0d117f41fcbec5c2bb08", nullable = true)
	String headCommitHash,

	@ArraySchema(schema = @Schema(description = "브랜치명", example = "main"))
	List<String> branches
) {
}
