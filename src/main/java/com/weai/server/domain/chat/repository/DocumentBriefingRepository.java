package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.BriefingStatus;
import com.weai.server.domain.chat.domain.DocumentBriefing;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentBriefingRepository extends JpaRepository<DocumentBriefing, Long> {

	@EntityGraph(attributePaths = {"document", "creator"})
	Optional<DocumentBriefing> findTopByDocument_IdOrderByCreatedAtDescIdDesc(Long documentId);

	@EntityGraph(attributePaths = {"document", "creator"})
	@Query("""
		select db
		from DocumentBriefing db
		where db.project.id = :projectId
		  and db.document.deletedAt is null
		  and (:status is null or db.status = :status)
		  and (
		    :keyword is null
		    or lower(db.document.originalFileName) like lower(concat('%', :keyword, '%'))
		    or lower(db.summary) like lower(concat('%', :keyword, '%'))
		  )
		""")
	Page<DocumentBriefing> findPageByProjectIdAndFilters(
		@Param("projectId") Long projectId,
		@Param("keyword") String keyword,
		@Param("status") BriefingStatus status,
		Pageable pageable
	);
}
