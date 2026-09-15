package com.weai.server.domain.smartcommit.repository;

import com.weai.server.domain.smartcommit.domain.SynPendingChange;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SynPendingChangeRepository extends JpaRepository<SynPendingChange, Long> {

	List<SynPendingChange> findByProject_IdOrderByFilePathAsc(Long projectId);

	Optional<SynPendingChange> findByProject_IdAndFilePath(Long projectId, String filePath);

	long countByProject_Id(Long projectId);

	@Query("select distinct p.project.id from SynPendingChange p")
	List<Long> findDistinctProjectIds();
}
