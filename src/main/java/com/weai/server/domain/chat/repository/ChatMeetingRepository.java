package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.ChatMeeting;
import com.weai.server.domain.chat.domain.MeetingStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMeetingRepository extends JpaRepository<ChatMeeting, Long> {

	@EntityGraph(attributePaths = {"hostUser", "chatRoom"})
	Optional<ChatMeeting> findByIdAndProject_Id(Long id, Long projectId);

	Optional<ChatMeeting> findTopByProject_IdAndStatusOrderByStartedAtDescIdDesc(Long projectId, MeetingStatus status);
}
