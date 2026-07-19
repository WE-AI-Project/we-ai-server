package com.weai.server.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weai.server.domain.notification.domain.Notification;
import com.weai.server.domain.notification.domain.NotificationTargetType;
import com.weai.server.domain.notification.domain.NotificationType;
import com.weai.server.domain.notification.repository.NotificationRepository;
import com.weai.server.domain.notification.response.NotificationDeleteResponse;
import com.weai.server.domain.notification.response.NotificationListResponse;
import com.weai.server.domain.notification.response.NotificationReadAllResponse;
import com.weai.server.domain.notification.response.NotificationReadResponse;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void getMyNotificationsReturnsUnreadCountAndExcludesDeletedNotifications() {
		TestFixture fixture = createFixture();
		Notification unread = saveNotification(fixture.project(), fixture.leader(), NotificationType.SCHEDULE, "Unread");
		Notification read = saveNotification(fixture.project(), fixture.leader(), NotificationType.PROJECT, "Read");
		read.markAsRead(LocalDateTime.now());
		Notification deleted = saveNotification(fixture.project(), fixture.leader(), NotificationType.SYSTEM, "Deleted");
		deleted.delete(LocalDateTime.now());
		notificationRepository.flush();

		NotificationListResponse response = notificationService.getMyNotifications(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			0,
			20,
			null,
			null
		);

		assertThat(response.projectId()).isEqualTo(fixture.project().getId());
		assertThat(response.unreadCount()).isEqualTo(1);
		assertThat(response.totalCount()).isEqualTo(2);
		assertThat(response.notifications())
			.extracting(NotificationListResponse.NotificationResponse::notificationId)
			.containsExactlyInAnyOrder(unread.getId(), read.getId())
			.doesNotContain(deleted.getId());
	}

	@Test
	void readNotificationIsIdempotent() {
		TestFixture fixture = createFixture();
		Notification notification = saveNotification(fixture.project(), fixture.leader(), NotificationType.SCHEDULE, "Read me");

		NotificationReadResponse firstResponse = notificationService.readNotification(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			notification.getId()
		);
		NotificationReadResponse secondResponse = notificationService.readNotification(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			notification.getId()
		);

		assertThat(firstResponse.isRead()).isTrue();
		assertThat(firstResponse.readAt()).isNotNull();
		assertThat(secondResponse.isRead()).isTrue();
		assertThat(secondResponse.readAt()).isEqualTo(firstResponse.readAt());
	}

	@Test
	void readAllNotificationsMarksOnlyCurrentUsersUnreadNotifications() {
		TestFixture fixture = createFixture();
		saveNotification(fixture.project(), fixture.leader(), NotificationType.SCHEDULE, "Leader unread 1");
		saveNotification(fixture.project(), fixture.leader(), NotificationType.QA, "Leader unread 2");
		saveNotification(fixture.project(), fixture.member(), NotificationType.SYSTEM, "Member unread");

		NotificationReadAllResponse response = notificationService.readAllNotifications(
			fixture.leader().getEmail(),
			fixture.project().getId()
		);
		NotificationListResponse leaderList = notificationService.getMyNotifications(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			0,
			20,
			null,
			null
		);
		NotificationListResponse memberList = notificationService.getMyNotifications(
			fixture.member().getEmail(),
			fixture.project().getId(),
			0,
			20,
			null,
			null
		);

		assertThat(response.updatedCount()).isEqualTo(2);
		assertThat(response.unreadCount()).isZero();
		assertThat(leaderList.unreadCount()).isZero();
		assertThat(memberList.unreadCount()).isEqualTo(1);
	}

	@Test
	void deleteNotificationSoftDeletesAndListExcludesIt() {
		TestFixture fixture = createFixture();
		Notification notification = saveNotification(fixture.project(), fixture.leader(), NotificationType.SYSTEM, "Delete me");

		NotificationDeleteResponse deleteResponse = notificationService.deleteNotification(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			notification.getId()
		);
		NotificationListResponse listResponse = notificationService.getMyNotifications(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			0,
			20,
			null,
			null
		);

		assertThat(deleteResponse.deleted()).isTrue();
		assertThat(notificationRepository.findById(notification.getId()).orElseThrow().getDeletedAt()).isNotNull();
		assertThat(listResponse.totalCount()).isZero();
	}

	@Test
	void readNotificationRejectsOtherUsersNotification() {
		TestFixture fixture = createFixture();
		Notification memberNotification = saveNotification(
			fixture.project(),
			fixture.member(),
			NotificationType.SCHEDULE,
			"Member only"
		);

		assertThatThrownBy(() -> notificationService.readNotification(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			memberNotification.getId()
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
	}

	@Test
	void getMyNotificationsRejectsNonProjectMember() {
		TestFixture fixture = createFixture();

		assertThatThrownBy(() -> notificationService.getMyNotifications(
			fixture.outsider().getEmail(),
			fixture.project().getId(),
			0,
			20,
			null,
			null
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
	}

	@Test
	void getMyNotificationsRejectsMissingProject() {
		TestFixture fixture = createFixture();

		assertThatThrownBy(() -> notificationService.getMyNotifications(
			fixture.leader().getEmail(),
			999_999L,
			0,
			20,
			null,
			null
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
	}

	private TestFixture createFixture() {
		String suffix = UUID.randomUUID().toString();
		User leader = userRepository.save(User.create(
			"leader-" + suffix,
			"password",
			"Leader",
			"leader-" + suffix + "@example.com",
			UserRole.USER
		));
		User member = userRepository.save(User.create(
			"member-" + suffix,
			"password",
			"Member",
			"member-" + suffix + "@example.com",
			UserRole.USER
		));
		User outsider = userRepository.save(User.create(
			"outsider-" + suffix,
			"password",
			"Outsider",
			"outsider-" + suffix + "@example.com",
			UserRole.USER
		));
		Project project = projectRepository.save(Project.create(
			"Notification Project " + suffix.substring(0, 8),
			"Notification service test project",
			suffix.substring(0, 8).toUpperCase(),
			"C:\\WE_AI\\notification-test",
			LocalDate.now(),
			LocalDate.now().plusDays(30),
			leader
		));
		projectMemberRepository.save(ProjectMember.leader(project, leader, ProjectDepartment.BACKEND));
		projectMemberRepository.save(ProjectMember.member(project, member, ProjectDepartment.FRONTEND));
		projectMemberRepository.flush();
		return new TestFixture(project, leader, member, outsider);
	}

	private Notification saveNotification(Project project, User receiver, NotificationType type, String title) {
		return notificationRepository.save(Notification.create(
			project,
			receiver,
			type,
			title,
			title + " message",
			NotificationTargetType.PROJECT,
			project.getId(),
			"/projects/" + project.getId()
		));
	}

	private record TestFixture(Project project, User leader, User member, User outsider) {
	}
}
