package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.MeetingParticipant;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

	long countByMeeting_Id(Long meetingId);

	boolean existsByMeeting_IdAndUser_Id(Long meetingId, Long userId);

	long countByMeeting_IdAndUser_IdIn(Long meetingId, Collection<Long> userIds);
}
