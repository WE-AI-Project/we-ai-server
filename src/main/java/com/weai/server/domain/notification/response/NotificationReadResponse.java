package com.weai.server.domain.notification.response;

import com.weai.server.domain.notification.domain.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "알림 읽음 처리 응답")
public record NotificationReadResponse(
	@Schema(description = "알림 ID", example = "100")
	Long notificationId,

	@Schema(description = "읽음 여부", example = "true")
	boolean isRead,

	@Schema(description = "읽은 시각", example = "2026-05-25T11:00:00")
	LocalDateTime readAt
) {

	public static NotificationReadResponse from(Notification notification) {
		return new NotificationReadResponse(notification.getId(), notification.isRead(), notification.getReadAt());
	}
}
