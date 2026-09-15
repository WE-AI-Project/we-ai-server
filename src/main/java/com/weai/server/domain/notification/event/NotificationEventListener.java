package com.weai.server.domain.notification.event;

import com.weai.server.domain.notification.domain.Notification;
import com.weai.server.domain.notification.repository.NotificationRepository;
import com.weai.server.domain.notification.response.NotificationListResponse.NotificationResponse;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link NotificationRequestedEvent}s published from business services and turns each
 * one into a persisted {@code Notification} row plus a real-time STOMP push, mirroring the
 * pattern chat already uses ({@code /topic/projects/{projectId}/chat-rooms/{chatRoomId}}).
 *
 * Runs synchronously on the publisher's thread/transaction: a save failure for one receiver is
 * caught and logged so it never rolls back the business operation that triggered it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

	private final NotificationRepository notificationRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate simpMessagingTemplate;

	@EventListener
	public void handleNotificationRequested(NotificationRequestedEvent event) {
		if (event.receiverUserIds() == null || event.receiverUserIds().isEmpty()) {
			return;
		}

		Project project = projectRepository.getReferenceById(event.projectId());

		event.receiverUserIds().stream().distinct().forEach(receiverUserId ->
			createAndPushNotification(project, receiverUserId, event));
	}

	private void createAndPushNotification(Project project, Long receiverUserId, NotificationRequestedEvent event) {
		try {
			User receiver = userRepository.getReferenceById(receiverUserId);
			Notification notification = notificationRepository.save(Notification.create(
				project,
				receiver,
				event.type(),
				event.title(),
				event.message(),
				event.targetType(),
				event.targetId(),
				event.linkUrl()
			));

			simpMessagingTemplate.convertAndSend(
				"/topic/projects/" + event.projectId() + "/notifications/" + receiverUserId,
				NotificationResponse.from(notification)
			);
		} catch (RuntimeException exception) {
			log.warn(
				"Failed to create/push notification for user {} in project {} (type={})",
				receiverUserId,
				event.projectId(),
				event.type(),
				exception
			);
		}
	}
}
