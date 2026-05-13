package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ProjectTechStack;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTechStackRepository extends JpaRepository<ProjectTechStack, Long> {

	List<ProjectTechStack> findByProject_IdInOrderByProject_IdAscIdAsc(Collection<Long> projectIds);

	List<ProjectTechStack> findByProject_IdOrderByCategoryAscIdAsc(Long projectId);
}
