package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.ChatRoomMember;
import com.weai.server.domain.chat.domain.ChatRoomMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

	Optional<ChatRoomMember> findByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);

	Optional<ChatRoomMember> findByChatRoom_IdAndUser_IdAndStatus(
		Long chatRoomId,
		Long userId,
		ChatRoomMemberStatus status
	);

	long countByChatRoom_IdAndStatus(Long chatRoomId, ChatRoomMemberStatus status);

	@Query("""
		select crm
		from ChatRoomMember crm
		where crm.chatRoom.project.id = :projectId
		  and crm.user.id = :userId
		  and crm.status = com.weai.server.domain.chat.domain.ChatRoomMemberStatus.ACTIVE
		""")
	List<ChatRoomMember> findActiveByProjectIdAndUserId(
		@Param("projectId") Long projectId,
		@Param("userId") Long userId
	);
}
