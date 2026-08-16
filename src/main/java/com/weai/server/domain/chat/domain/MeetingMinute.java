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
	name = "meeting_minutes",
	indexes = {
		@Index(name = "idx_meeting_minutes_project_created", columnList = "project_id,created_at"),
		@Index(name = "idx_meeting_minutes_meeting", columnList = "meeting_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MeetingMinute extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "minute_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", nullable = false)
	private ChatMeeting meeting;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "writer_id", nullable = false)
	private User writer;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String content;

	@Column(length = 2000)
	private String summary;

	@Column(name = "action_items", columnDefinition = "LONGTEXT")
	private String actionItems;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MeetingMinuteStatus status;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;
}
