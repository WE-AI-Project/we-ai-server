package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ProjectEnvironmentSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectEnvironmentSettingRepository extends JpaRepository<ProjectEnvironmentSetting, Long> {

	Optional<ProjectEnvironmentSetting> findByProject_Id(Long projectId);
}
