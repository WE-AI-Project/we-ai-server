package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Git 브랜치 그래프 조회 결과")
public record ProjectGitBranchGraphResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,
	@Schema(description = "현재 브랜치명. detached HEAD이면 HEAD", example = "main", nullable = true)
	String currentBranch,
	@ArraySchema(schema = @Schema(implementation = BranchResponse.class))
	List<BranchResponse> branches,
	@ArraySchema(schema = @Schema(implementation = CommitNodeResponse.class))
	List<CommitNodeResponse> nodes,
	@ArraySchema(schema = @Schema(implementation = CommitEdgeResponse.class))
	List<CommitEdgeResponse> edges
) {
	@Schema(description = "브랜치 정보")
	public record BranchResponse(
		@Schema(description = "브랜치명", example = "main") String name,
		@Schema(description = "현재 브랜치 여부") boolean current,
		@Schema(description = "원격 브랜치 여부") boolean remote,
		@Schema(description = "브랜치 최근 커밋 해시", nullable = true) String lastCommitHash,
		@Schema(description = "브랜치 최근 커밋 메시지", nullable = true) String lastCommitMessage
	) {
	}

	@Schema(description = "커밋 그래프 노드")
	public record CommitNodeResponse(
		@Schema(description = "커밋 전체 해시") String commitHash,
		@Schema(description = "커밋 단축 해시") String shortCommitHash,
		@Schema(description = "커밋 메시지") String message,
		@Schema(description = "작성자명") String authorName,
		@Schema(description = "작성자 이메일") String authorEmail,
		@Schema(description = "커밋 시각") LocalDateTime committedAt,
		@ArraySchema(schema = @Schema(description = "이 커밋을 포함하는 브랜치명")) List<String> branchNames,
		@Schema(description = "그래프 X 좌표", nullable = true) Integer x,
		@Schema(description = "그래프 Y 좌표", example = "0") Integer y
	) {
	}

	@Schema(description = "커밋 그래프 간선")
	public record CommitEdgeResponse(
		@Schema(description = "부모 커밋 해시") String from,
		@Schema(description = "자식 커밋 해시") String to,
		@Schema(description = "간선 유형", example = "PARENT") String type
	) {
	}
}
