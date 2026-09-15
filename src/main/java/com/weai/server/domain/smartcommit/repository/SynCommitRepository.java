package com.weai.server.domain.smartcommit.repository;

import com.weai.server.domain.smartcommit.domain.SynCommit;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SynCommitRepository extends JpaRepository<SynCommit, Long> {

	Page<SynCommit> findByProject_Id(Long projectId, Pageable pageable);

	Optional<SynCommit> findByIdAndProject_Id(Long id, Long projectId);

	Optional<SynCommit> findTopByProject_IdOrderByCommittedAtDesc(Long projectId);
}
