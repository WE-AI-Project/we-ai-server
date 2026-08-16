package com.weai.server.domain.chat.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "회의 모드 종료 및 회의록 저장 요청")
public record MeetingEndRequest(
	@Schema(description = "회의록 원문", example = "오늘 회의에서는 문서 업로드 API와 회의록 저장 API 구현 범위를 정리하였다.")
	String content,

	@Schema(description = "회의 요약", example = "채팅 문서/회의 기능 구현 범위를 확정하였다.")
	String summary,

	@ArraySchema(schema = @Schema(description = "해야 할 일", example = "문서 업로드 API 구현"))
	List<String> actionItems,

	@ArraySchema(schema = @Schema(description = "참여자 userId", example = "1"))
	List<Long> participants
) {
}
