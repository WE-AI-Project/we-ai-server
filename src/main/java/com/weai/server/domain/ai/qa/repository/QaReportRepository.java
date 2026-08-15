package com.weai.server.domain.ai.qa.repository;

import com.weai.server.domain.ai.qa.domain.QaReport;
import com.weai.server.domain.ai.qa.domain.QaReportStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QaReportRepository extends JpaRepository<QaReport, Long> {

	@Query("""
		select qr
		from QaReport qr
		left join fetch qr.qaRun
		where qr.id = :qaReportId
		  and qr.project.id = :projectId
		""")
	Optional<QaReport> findDetailByIdAndProjectId(
		@Param("qaReportId") Long qaReportId,
		@Param("projectId") Long projectId
	);

	@Query("""
		select qr
		from QaReport qr
		left join fetch qr.qaRun
		left join fetch qr.issues
		where qr.id = :qaReportId
		  and qr.project.id = :projectId
		""")
	Optional<QaReport> findDetailWithIssuesByIdAndProjectId(
		@Param("qaReportId") Long qaReportId,
		@Param("projectId") Long projectId
	);

	@EntityGraph(attributePaths = {"qaRun"})
	@Query("""
		select qr
		from QaReport qr
		where qr.project.id = :projectId
		  and (:status is null or qr.status = :status)
		  and (:commitId is null or qr.commitId = :commitId or qr.commitHash = :commitId)
		""")
	Page<QaReport> findPageByProjectIdAndFilters(
		@Param("projectId") Long projectId,
		@Param("status") QaReportStatus status,
		@Param("commitId") String commitId,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {"qaRun"})
	@Query("""
		select qr
		from QaReport qr
		where qr.project.id = :projectId
		  and (qr.commitId = :commitId or qr.commitHash = :commitId)
		order by qr.createdAt desc, qr.id desc
		""")
	List<QaReport> findByProjectIdAndCommitIdOrderByLatest(
		@Param("projectId") Long projectId,
		@Param("commitId") String commitId
	);
}
