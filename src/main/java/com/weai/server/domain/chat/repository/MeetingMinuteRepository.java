package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.MeetingMinute;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingMinuteRepository extends JpaRepository<MeetingMinute, Long> {

	@EntityGraph(attributePaths = {"meeting", "writer"})
	@Query("""
		select mm
		from MeetingMinute mm
		where mm.project.id = :projectId
		  and mm.deletedAt is null
		  and (:keyword is null or lower(mm.title) like lower(concat('%', :keyword, '%'))
		    or lower(mm.summary) like lower(concat('%', :keyword, '%')))
		  and (:startAt is null or mm.createdAt >= :startAt)
		  and (:endAt is null or mm.createdAt <= :endAt)
		""")
	Page<MeetingMinute> findPageByProjectIdAndFilters(
		@Param("projectId") Long projectId,
		@Param("keyword") String keyword,
		@Param("startAt") LocalDateTime startAt,
		@Param("endAt") LocalDateTime endAt,
		Pageable pageable
	);
}
