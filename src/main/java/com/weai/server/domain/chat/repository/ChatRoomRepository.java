package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.ChatRoom;
import com.weai.server.domain.chat.domain.ChatRoomType;
import com.weai.server.domain.project.domain.ProjectDepartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	@Query(
		value = """
			select cr
			from ChatRoom cr
			where cr.project.id = :projectId
			  and cr.status = com.weai.server.domain.chat.domain.ChatRoomStatus.ACTIVE
			  and cr.deletedAt is null
			  and (:type is null or cr.type = :type)
			  and (:department is null or cr.department = :department)
			  and (:keyword is null or lower(cr.name) like lower(concat('%', :keyword, '%')))
			  and (
			    cr.isPrivate = false
			    or exists (
			      select 1
			      from ChatRoomMember crm
			      where crm.chatRoom = cr
			        and crm.user.id = :userId
			        and crm.status = com.weai.server.domain.chat.domain.ChatRoomMemberStatus.ACTIVE
			    )
			  )
			order by
			  coalesce((
			    select max(cm.createdAt)
			    from ChatMessage cm
			    where cm.chatRoom = cr
			      and cm.deletedAt is null
			  ), cr.createdAt) desc,
			  cr.id desc
			""",
		countQuery = """
			select count(cr)
			from ChatRoom cr
			where cr.project.id = :projectId
			  and cr.status = com.weai.server.domain.chat.domain.ChatRoomStatus.ACTIVE
			  and cr.deletedAt is null
			  and (:type is null or cr.type = :type)
			  and (:department is null or cr.department = :department)
			  and (:keyword is null or lower(cr.name) like lower(concat('%', :keyword, '%')))
			  and (
			    cr.isPrivate = false
			    or exists (
			      select 1
			      from ChatRoomMember crm
			      where crm.chatRoom = cr
			        and crm.user.id = :userId
			        and crm.status = com.weai.server.domain.chat.domain.ChatRoomMemberStatus.ACTIVE
			    )
			  )
			"""
	)
	Page<ChatRoom> findAccessibleRooms(
		@Param("projectId") Long projectId,
		@Param("userId") Long userId,
		@Param("type") ChatRoomType type,
		@Param("department") ProjectDepartment department,
		@Param("keyword") String keyword,
		Pageable pageable
	);
}
