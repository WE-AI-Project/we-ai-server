package com.weai.server.domain.notification.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전체 알림 읽음 처리 응답")
public record NotificationReadAllResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "읽음 처리된 알림 수", example = "5")
	int updatedCount,

	@Schema(description = "남은 읽지 않은 알림 수", example = "0")
	long unreadCount
) {
}
