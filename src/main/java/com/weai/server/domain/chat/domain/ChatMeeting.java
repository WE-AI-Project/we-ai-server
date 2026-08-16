package com.weai.server.domain.chat.domain;

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
	name = "chat_meetings",
	indexes = {
		@Index(name = "idx_chat_meetings_project_status", columnList = "project_id,status"),
		@Index(name = "idx_chat_meetings_project_started", columnList = "project_id,started_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatMeeting extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "meeting_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chat_room_id")
	private ChatRoom chatRoom;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "host_user_id", nullable = false)
	private User hostUser;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MeetingStatus status;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "ended_at")
	private LocalDateTime endedAt;

	public static ChatMeeting start(Project project, ChatRoom chatRoom, User hostUser, String title, String description) {
		LocalDateTime now = LocalDateTime.now();
		return ChatMeeting.builder()
			.project(project)
			.chatRoom(chatRoom)
			.hostUser(hostUser)
			.title(title)
			.description(description)
			.status(MeetingStatus.IN_PROGRESS)
			.startedAt(now)
			.build();
	}

	public void end(LocalDateTime endedAt) {
		this.status = MeetingStatus.ENDED;
		this.endedAt = endedAt;
	}
}
