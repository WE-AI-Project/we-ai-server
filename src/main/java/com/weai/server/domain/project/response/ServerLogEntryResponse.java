package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "서버 실시간 로그 항목 응답")
public record ServerLogEntryResponse(
	@Schema(description = "로그 시퀀스 ID", example = "101")
	long id,

	@Schema(description = "로그 발생 시각", example = "10:38:41.123")
	String time,

	@Schema(description = "로그 레벨 (INFO, WARN, ERROR, DEBUG, TRACE)", example = "INFO")
	String level,

	@Schema(description = "실행 스레드명", example = "http-nio-8080-exec-1")
	String thread,

	@Schema(description = "로거 이름 / 클래스명", example = "com.weai.server.WeAiServerApplication")
	String logger,

	@Schema(description = "로그 메시지 본문", example = "Started WeAiServerApplication in 3.125 seconds")
	String message
) {
}
