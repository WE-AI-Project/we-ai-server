package com.weai.server.domain.notification.event;

import com.weai.server.domain.notification.domain.NotificationTargetType;
import com.weai.server.domain.notification.domain.NotificationType;
import java.util.List;

/**
 * Published by business services whenever a real event (member joined, schedule changed, QA
 * finished, ...) should surface as an in-app notification. {@link NotificationEventListener}
 * is the sole consumer: it persists a {@code Notification} row per receiver and pushes it over
 * STOMP so the frontend does not have to poll.
 */
public record NotificationRequestedEvent(
	Long projectId,
	List<Long> receiverUserIds,
	NotificationType type,
	String title,
	String message,
	NotificationTargetType targetType,
	Long targetId,
	String linkUrl
) {
}
