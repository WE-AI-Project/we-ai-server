package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.BuildRun;
import com.weai.server.domain.project.domain.BuildRunStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BuildRunRepository extends JpaRepository<BuildRun, Long>, JpaSpecificationExecutor<BuildRun> {

	Optional<BuildRun> findByBuildRunIdAndProject_Id(Long buildRunId, Long projectId);

	boolean existsByProject_IdAndStatusIn(Long projectId, Collection<BuildRunStatus> statuses);

	long countByProject_IdAndStatus(Long projectId, BuildRunStatus status);
}
