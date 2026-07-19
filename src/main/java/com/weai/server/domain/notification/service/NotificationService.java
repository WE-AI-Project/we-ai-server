package com.weai.server.domain.notification.service;

import com.weai.server.domain.notification.domain.Notification;
import com.weai.server.domain.notification.domain.NotificationType;
import com.weai.server.domain.notification.repository.NotificationRepository;
import com.weai.server.domain.notification.response.NotificationDeleteResponse;
import com.weai.server.domain.notification.response.NotificationListResponse;
import com.weai.server.domain.notification.response.NotificationReadAllResponse;
import com.weai.server.domain.notification.response.NotificationReadResponse;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final NotificationRepository notificationRepository;
	private final ProjectService projectService;
	private final UserService userService;

	public NotificationListResponse getMyNotifications(
		String userEmail,
		Long projectId,
		Integer page,
		Integer size,
		NotificationType type,
		Boolean isRead
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());

		Pageable pageable = normalizePageable(page, size);
		Page<Notification> notificationPage = notificationRepository.findByProjectIdAndReceiverUserIdWithFilters(
			projectId,
			user.getId(),
			type,
			isRead,
			pageable
		);
		long unreadCount = notificationRepository.countByProject_IdAndReceiver_IdAndIsReadFalseAndDeletedAtIsNull(
			projectId,
			user.getId()
		);

		return NotificationListResponse.from(projectId, unreadCount, notificationPage);
	}

	@Transactional
	public NotificationReadResponse readNotification(String userEmail, Long projectId, Long notificationId) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		Notification notification = getUserNotification(projectId, user.getId(), notificationId);

		try {
			notification.markAsRead(LocalDateTime.now());
			return NotificationReadResponse.from(notificationRepository.saveAndFlush(notification));
		} catch (DataAccessException exception) {
			throw new ApiException(ErrorCode.NOTIFICATION_READ_FAILED, "Failed to mark the notification as read.");
		}
	}

	@Transactional
	public NotificationReadAllResponse readAllNotifications(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		List<Notification> unreadNotifications =
			notificationRepository.findByProject_IdAndReceiver_IdAndIsReadFalseAndDeletedAtIsNull(projectId, user.getId());

		try {
			LocalDateTime now = LocalDateTime.now();
			unreadNotifications.forEach(notification -> notification.markAsRead(now));
			notificationRepository.saveAll(unreadNotifications);
			notificationRepository.flush();
		} catch (DataAccessException exception) {
			throw new ApiException(ErrorCode.NOTIFICATION_READ_FAILED, "Failed to mark all notifications as read.");
		}

		long unreadCount = notificationRepository.countByProject_IdAndReceiver_IdAndIsReadFalseAndDeletedAtIsNull(
			projectId,
			user.getId()
		);
		return new NotificationReadAllResponse(projectId, unreadNotifications.size(), unreadCount);
	}

	@Transactional
	public NotificationDeleteResponse deleteNotification(String userEmail, Long projectId, Long notificationId) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
		Notification notification = getUserNotification(projectId, user.getId(), notificationId);

		try {
			notification.delete(LocalDateTime.now());
			notificationRepository.saveAndFlush(notification);
		} catch (DataAccessException exception) {
			throw new ApiException(ErrorCode.NOTIFICATION_DELETE_FAILED, "Failed to delete the notification.");
		}

		return NotificationDeleteResponse.from(notificationId);
	}

	private Notification getUserNotification(Long projectId, Long userId, Long notificationId) {
		return notificationRepository.findByIdAndProject_IdAndReceiver_IdAndDeletedAtIsNull(
			notificationId,
			projectId,
			userId
		).orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));
	}

	private Pageable normalizePageable(Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		if (resolvedPage < 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "page must be greater than or equal to 0.");
		}
		if (resolvedSize <= 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "size must be greater than 0.");
		}

		int limitedSize = Math.min(resolvedSize, MAX_SIZE);
		Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
		return PageRequest.of(resolvedPage, limitedSize, sort);
	}
}
