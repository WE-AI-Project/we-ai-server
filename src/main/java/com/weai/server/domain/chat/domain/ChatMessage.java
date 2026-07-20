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
	name = "chat_messages",
	indexes = {
		@Index(name = "idx_chat_messages_room_created", columnList = "chat_room_id, created_at"),
		@Index(name = "idx_chat_messages_room_id", columnList = "chat_room_id, chat_message_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatMessage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_message_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chat_room_id", nullable = false)
	private ChatRoom chatRoom;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sender_id", nullable = false)
	private User sender;

	@Column(nullable = false, length = 2000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "message_type", nullable = false, length = 30)
	private ChatMessageType messageType;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static ChatMessage text(ChatRoom chatRoom, User sender, String content) {
		return create(chatRoom, sender, content, ChatMessageType.TEXT);
	}

	public static ChatMessage create(ChatRoom chatRoom, User sender, String content, ChatMessageType messageType) {
		return ChatMessage.builder()
			.chatRoom(chatRoom)
			.sender(sender)
			.content(content)
			.messageType(messageType)
			.build();
	}

	public void delete(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
}
