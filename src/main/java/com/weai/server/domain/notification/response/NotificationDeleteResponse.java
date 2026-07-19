package com.weai.server.domain.notification.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 삭제 응답")
public record NotificationDeleteResponse(
	@Schema(description = "삭제된 알림 ID", example = "100")
	Long notificationId,

	@Schema(description = "삭제 여부", example = "true")
	boolean deleted
) {

	public static NotificationDeleteResponse from(Long notificationId) {
		return new NotificationDeleteResponse(notificationId, true);
	}
}
