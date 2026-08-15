package com.weai.server.domain.ai.qa.repository;

import com.weai.server.domain.ai.qa.domain.QaRun;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaRunRepository extends JpaRepository<QaRun, Long> {

	Optional<QaRun> findByIdAndProject_Id(Long id, Long projectId);
}
