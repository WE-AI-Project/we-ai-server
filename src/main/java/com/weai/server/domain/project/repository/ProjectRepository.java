package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	boolean existsByProjectCode(String projectCode);

	Optional<Project> findByProjectCode(String projectCode);

	List<Project> findByCreatedBy_IdAndStatusOrderByCreatedAtDescIdDesc(Long userId, ProjectStatus status);
}
