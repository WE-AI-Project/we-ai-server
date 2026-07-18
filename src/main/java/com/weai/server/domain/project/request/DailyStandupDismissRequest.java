package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "오늘 다시 보지 않기 저장 요청")
public record DailyStandupDismissRequest(
	@Schema(description = "오늘 다시 보지 않기 여부입니다. 생략해도 저장됩니다.", example = "true")
	Boolean dismiss
) {
}
