package com.weai.server.domain.notification.config;

import com.weai.server.domain.notification.domain.Notification;
import com.weai.server.domain.notification.domain.NotificationTargetType;
import com.weai.server.domain.notification.domain.NotificationType;
import com.weai.server.domain.notification.repository.NotificationRepository;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.security.config.DefaultAuthProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class DevNotificationSeedInitializer implements ApplicationRunner {

	private static final String PROJECT_CODE = "DEV-NOTIFICATION";

	private final DefaultAuthProperties authProperties;
	private final UserService userService;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final NotificationRepository notificationRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		userService.ensureBootstrapAdmin(authProperties.getUsername(), authProperties.getPassword());

		User receiver = userRepository.findByUsername(authProperties.getUsername()).orElseThrow();
		Project project = projectRepository.findByProjectCode(PROJECT_CODE)
			.orElseGet(() -> projectRepository.save(Project.create(
				"Notification API Test Project",
				"Development seed data for notification list, delete, and read-all API testing.",
				PROJECT_CODE,
				"https://github.com/we-ai/dev-notification-seed",
				"C:\\WE_AI\\notification-dev",
				LocalDate.now(),
				LocalDate.now().plusDays(14),
				receiver
			)));

		userRepository.findAll().forEach(user -> seedUserNotifications(project, user));
	}

	private void seedUserNotifications(Project project, User receiver) {
		projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), receiver.getId())
			.orElseGet(() -> projectMemberRepository.save(createProjectMember(project, receiver)));

		seedNotification(
			project,
			receiver,
			NotificationType.PROJECT,
			"Project invitation received",
			"Sample project notification for list API testing.",
			NotificationTargetType.PROJECT,
			project.getId(),
			"/projects/%d".formatted(project.getId()),
			false
		);
		seedNotification(
			project,
			receiver,
			NotificationType.SCHEDULE,
			"Schedule status changed",
			"Unread schedule notification for read-all API testing.",
			NotificationTargetType.SCHEDULE,
			1L,
			"/projects/%d/schedules/1".formatted(project.getId()),
			false
		);
		seedNotification(
			project,
			receiver,
			NotificationType.SCHEDULE,
			"Schedule review completed",
			"Already-read schedule notification for SCHEDULE and isRead=true filter testing.",
			NotificationTargetType.SCHEDULE,
			2L,
			"/projects/%d/schedules/2".formatted(project.getId()),
			true
		);
		seedNotification(
			project,
			receiver,
			NotificationType.MEMBER,
			"New member joined",
			"Member notification that can be used to verify delete API behavior.",
			NotificationTargetType.MEMBER,
			receiver.getId(),
			"/projects/%d/members".formatted(project.getId()),
			false
		);
		seedNotification(
			project,
			receiver,
			NotificationType.QA,
			"QA answer posted",
			"QA notification for type and read-state filter checks.",
			NotificationTargetType.QA,
			1L,
			"/projects/%d/qa/1".formatted(project.getId()),
			false
		);
		seedNotification(
			project,
			receiver,
			NotificationType.SYSTEM,
			"System maintenance notice",
			"Already-read system notification for mixed read-state list testing.",
			NotificationTargetType.SYSTEM,
			null,
			"/projects/%d/notifications".formatted(project.getId()),
			true
		);
	}

	private ProjectMember createProjectMember(Project project, User receiver) {
		if (project.getCreatedBy().getId().equals(receiver.getId())) {
			return ProjectMember.leader(project, receiver, ProjectDepartment.BACKEND);
		}
		return ProjectMember.member(project, receiver, ProjectDepartment.BACKEND);
	}

	private void seedNotification(
		Project project,
		User receiver,
		NotificationType type,
		String title,
		String message,
		NotificationTargetType targetType,
		Long targetId,
		String linkUrl,
		boolean read
	) {
		if (notificationRepository.existsByProject_IdAndReceiver_IdAndTitleAndDeletedAtIsNull(
			project.getId(),
			receiver.getId(),
			title
		)) {
			return;
		}

		Notification notification = Notification.create(project, receiver, type, title, message, targetType, targetId, linkUrl);
		if (read) {
			notification.markAsRead(LocalDateTime.now());
		}
		notificationRepository.save(notification);
	}
}
