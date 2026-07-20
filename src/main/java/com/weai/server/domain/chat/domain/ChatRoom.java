package com.weai.server.domain.chat.domain;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
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
	name = "chat_rooms",
	indexes = {
		@Index(name = "idx_chat_rooms_project_status", columnList = "project_id, status"),
		@Index(name = "idx_chat_rooms_project_type", columnList = "project_id, type"),
		@Index(name = "idx_chat_rooms_project_department", columnList = "project_id, department")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatRoom extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_room_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ChatRoomType type;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private ProjectDepartment department;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ChatRoomStatus status;

	@Column(name = "is_private", nullable = false)
	private boolean isPrivate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static ChatRoom create(
		Project project,
		String name,
		String description,
		ChatRoomType type,
		ProjectDepartment department,
		boolean isPrivate,
		User createdBy
	) {
		return ChatRoom.builder()
			.project(project)
			.name(name)
			.description(description)
			.type(type)
			.department(department)
			.status(ChatRoomStatus.ACTIVE)
			.isPrivate(isPrivate)
			.createdBy(createdBy)
			.build();
	}

	public boolean isActive() {
		return status == ChatRoomStatus.ACTIVE && deletedAt == null;
	}

	public void delete(LocalDateTime deletedAt) {
		this.status = ChatRoomStatus.DELETED;
		this.deletedAt = deletedAt;
	}
}
