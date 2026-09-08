package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "커밋 컨벤션 검사 결과")
public record ProjectGitCommitConventionCheckResponse(
	@Schema(description = "커밋 컨벤션 충족 여부", example = "true")
	boolean valid,
	@Schema(description = "커밋 타입", example = "feat", nullable = true)
	String type,
	@Schema(description = "커밋 스코프", example = "chat", nullable = true)
	String scope,
	@Schema(description = "커밋 제목", example = "채팅방 생성 API 구현", nullable = true)
	String subject,
	@Schema(description = "정규화된 커밋 메시지", example = "feat: 채팅방 생성 API 구현", nullable = true)
	String normalizedMessage,
	@ArraySchema(schema = @Schema(implementation = ConventionIssue.class))
	List<ConventionIssue> errors,
	@ArraySchema(schema = @Schema(implementation = ConventionIssue.class))
	List<ConventionIssue> warnings,
	@ArraySchema(schema = @Schema(example = "예: feat: 채팅방 생성 API 구현"))
	List<String> suggestions
) {
	@Schema(description = "커밋 컨벤션 검사 항목")
	public record ConventionIssue(
		@Schema(description = "오류 필드", example = "message")
		String field,
		@Schema(description = "오류 코드", example = "INVALID_COMMIT_MESSAGE_FORMAT")
		String code,
		@Schema(description = "오류 또는 경고 메시지", example = "커밋 메시지는 'type: subject' 형식이어야 합니다.")
		String message
	) {
	}
}
