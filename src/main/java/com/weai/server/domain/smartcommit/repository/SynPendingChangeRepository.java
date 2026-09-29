package com.weai.server.domain.smartcommit.repository;

import com.weai.server.domain.smartcommit.domain.SynPendingChange;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SynPendingChangeRepository extends JpaRepository<SynPendingChange, Long> {

	List<SynPendingChange> findByProject_IdOrderByFilePathAsc(Long projectId);

	/**
	 * Same as {@link #findByProject_IdOrderByFilePathAsc}, but takes a row-level write lock on
	 * every matched row for the rest of the current transaction. The manual and auto-commit drain
	 * paths both use this (never the unlocked variant) so that two drains racing for the same
	 * project serialize instead of both reading the same pending batch and each producing a
	 * {@code SynCommit} for it.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from SynPendingChange p where p.project.id = :projectId order by p.filePath asc")
	List<SynPendingChange> findByProject_IdOrderByFilePathAscForUpdate(@Param("projectId") Long projectId);

	Optional<SynPendingChange> findByProject_IdAndFilePath(Long projectId, String filePath);

	long countByProject_Id(Long projectId);

	@Query("select distinct p.project.id from SynPendingChange p")
	List<Long> findDistinctProjectIds();
}
