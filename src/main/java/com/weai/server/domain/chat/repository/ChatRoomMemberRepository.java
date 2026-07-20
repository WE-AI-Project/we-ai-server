package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.ChatRoomMember;
import com.weai.server.domain.chat.domain.ChatRoomMemberStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

	Optional<ChatRoomMember> findByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);

	Optional<ChatRoomMember> findByChatRoom_IdAndUser_IdAndStatus(
		Long chatRoomId,
		Long userId,
		ChatRoomMemberStatus status
	);

	long countByChatRoom_IdAndStatus(Long chatRoomId, ChatRoomMemberStatus status);
}
