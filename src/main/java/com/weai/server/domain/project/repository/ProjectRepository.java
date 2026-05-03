package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.Project;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	boolean existsByProjectCode(String projectCode);

	Optional<Project> findByProjectCode(String projectCode);
}
