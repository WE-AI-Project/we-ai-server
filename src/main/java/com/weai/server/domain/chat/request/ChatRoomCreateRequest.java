package com.weai.server.domain.chat.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 생성 요청")
public record ChatRoomCreateRequest(
	@Schema(description = "채팅방 이름(2~50자)", example = "백엔드 채팅방", requiredMode = Schema.RequiredMode.REQUIRED)
	String name,

	@Schema(description = "채팅방 유형(GENERAL 또는 DEPARTMENT)", example = "DEPARTMENT", requiredMode = Schema.RequiredMode.REQUIRED)
	String type,

	@Schema(description = "부서 채팅방의 부서. GENERAL일 때는 생략합니다.", example = "BACKEND")
	String department
) {
}
