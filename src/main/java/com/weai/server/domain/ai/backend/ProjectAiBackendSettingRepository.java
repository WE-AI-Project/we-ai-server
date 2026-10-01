package com.weai.server.domain.ai.backend;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectAiBackendSettingRepository extends JpaRepository<ProjectAiBackendSetting, Long> {
	Optional<ProjectAiBackendSetting> findByProject_Id(Long projectId);
}
