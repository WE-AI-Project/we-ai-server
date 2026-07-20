package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.ChatMessage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	Optional<ChatMessage> findTopByChatRoom_IdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long chatRoomId);

	long countByChatRoom_IdAndSender_IdNotAndDeletedAtIsNull(Long chatRoomId, Long senderId);

	long countByChatRoom_IdAndIdGreaterThanAndSender_IdNotAndDeletedAtIsNull(
		Long chatRoomId,
		Long lastReadMessageId,
		Long senderId
	);
}
