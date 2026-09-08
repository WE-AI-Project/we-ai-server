package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ServerLog;
import com.weai.server.domain.project.domain.ServerLogLevel;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServerLogRepository extends JpaRepository<ServerLog, Long>, JpaSpecificationExecutor<ServerLog> {

	Page<ServerLog> findByProject_IdAndDeletedAtIsNull(Long projectId, Pageable pageable);

	long countByProject_IdAndLevelAndDeletedAtIsNull(Long projectId, ServerLogLevel level);

	long countByProject_IdAndDeletedAtIsNull(Long projectId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ServerLog serverLog
		set serverLog.deletedAt = :deletedAt
		where serverLog.project.id = :projectId
		  and serverLog.deletedAt is null
		""")
	int softDeleteAllByProjectId(@Param("projectId") Long projectId, @Param("deletedAt") LocalDateTime deletedAt);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		delete from ServerLog serverLog
		where serverLog.deletedAt is not null
		  and serverLog.deletedAt < :cutoff
		""")
	int deleteAllSoftDeletedBefore(@Param("cutoff") LocalDateTime cutoff);
}
