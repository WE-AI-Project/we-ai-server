package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	Optional<ChatMessage> findTopByChatRoom_IdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long chatRoomId);

	long countByChatRoom_IdAndSender_IdNotAndDeletedAtIsNull(Long chatRoomId, Long senderId);

	long countByChatRoom_IdAndIdGreaterThanAndSender_IdNotAndDeletedAtIsNull(
		Long chatRoomId,
		Long lastReadMessageId,
		Long senderId
	);

	@Query("""
		select cm
		from ChatMessage cm
		join fetch cm.sender s
		where cm.chatRoom.id = :chatRoomId
		  and cm.deletedAt is null
		  and (:beforeMessageId is null or cm.id < :beforeMessageId)
		  and (:keyword is null or lower(cm.content) like lower(concat('%', :keyword, '%')))
		order by cm.createdAt desc, cm.id desc
		""")
	List<ChatMessage> findMessagesBefore(
		@Param("chatRoomId") Long chatRoomId,
		@Param("beforeMessageId") Long beforeMessageId,
		@Param("keyword") String keyword,
		Pageable pageable
	);
}
