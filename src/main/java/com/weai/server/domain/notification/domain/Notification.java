package com.weai.server.domain.notification.domain;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.user.domain.User;
import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "notifications",
	indexes = {
		@Index(name = "idx_notifications_project_receiver", columnList = "project_id, receiver_user_id"),
		@Index(name = "idx_notifications_receiver_read", columnList = "receiver_user_id, is_read"),
		@Index(name = "idx_notifications_deleted_at", columnList = "deleted_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Notification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "receiver_user_id", nullable = false)
	private User receiver;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationType type;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 500)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", length = 30)
	private NotificationTargetType targetType;

	@Column(name = "target_id")
	private Long targetId;

	@Column(name = "link_url", length = 500)
	private String linkUrl;

	@Column(name = "is_read", nullable = false)
	private boolean isRead;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static Notification create(
		Project project,
		User receiver,
		NotificationType type,
		String title,
		String message,
		NotificationTargetType targetType,
		Long targetId,
		String linkUrl
	) {
		return Notification.builder()
			.project(project)
			.receiver(receiver)
			.type(type)
			.title(title)
			.message(message)
			.targetType(targetType)
			.targetId(targetId)
			.linkUrl(linkUrl)
			.isRead(false)
			.build();
	}

	public void markAsRead(LocalDateTime readAt) {
		if (isRead) {
			return;
		}

		this.isRead = true;
		this.readAt = readAt;
	}

	public void delete(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
}
