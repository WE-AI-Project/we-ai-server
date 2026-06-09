package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ProjectMilestone;
import com.weai.server.domain.project.domain.ProjectMilestoneStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {

	List<ProjectMilestone> findByProject_IdOrderByStartDateAscIdAsc(Long projectId);

	List<ProjectMilestone> findByProject_IdAndStatusOrderByStartDateAscIdAsc(Long projectId, ProjectMilestoneStatus status);
}
