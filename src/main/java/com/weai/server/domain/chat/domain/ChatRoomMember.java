package com.weai.server.domain.chat.domain;

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
	name = "chat_room_members",
	indexes = {
		@Index(name = "idx_chat_room_members_room_user", columnList = "chat_room_id, user_id", unique = true),
		@Index(name = "idx_chat_room_members_user_status", columnList = "user_id, status")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatRoomMember extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_room_member_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chat_room_id", nullable = false)
	private ChatRoom chatRoom;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_read_message_id")
	private ChatMessage lastReadMessage;

	@Column(name = "joined_at", nullable = false)
	private LocalDateTime joinedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ChatRoomMemberStatus status;

	public static ChatRoomMember active(ChatRoom chatRoom, User user) {
		return active(chatRoom, user, null);
	}

	public static ChatRoomMember active(ChatRoom chatRoom, User user, ChatMessage lastReadMessage) {
		return ChatRoomMember.builder()
			.chatRoom(chatRoom)
			.user(user)
			.lastReadMessage(lastReadMessage)
			.joinedAt(LocalDateTime.now())
			.status(ChatRoomMemberStatus.ACTIVE)
			.build();
	}

	public void updateLastReadMessage(ChatMessage lastReadMessage) {
		this.lastReadMessage = lastReadMessage;
	}

	public void leave() {
		this.status = ChatRoomMemberStatus.LEFT;
	}
}
