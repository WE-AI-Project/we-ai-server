package com.weai.server.domain.chat.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 생성 요청")
public record ChatRoomCreateRequest(
	@Schema(description = "채팅방 이름(2~50자). 부서 채팅방은 생략하면 선택 부서명으로 자동 생성됩니다.", example = "백엔드 채팅방")
	String name,

	@Schema(description = "채팅방 유형(GENERAL 또는 DEPARTMENT)", example = "DEPARTMENT", requiredMode = Schema.RequiredMode.REQUIRED)
	String type,

	@Schema(description = "부서 채팅방의 부서. GENERAL일 때는 생략합니다.", example = "BACKEND")
	String department,

	@Schema(
		description = "부서 채팅방 공개 여부입니다. false면 프로젝트 전체 공개, true면 해당 부서 멤버만 접근합니다. 생략하면 기존 호환을 위해 true입니다.",
		example = "false"
	)
	Boolean isPrivate
) {

	public ChatRoomCreateRequest(String name, String type, String department) {
		this(name, type, department, null);
	}
}
