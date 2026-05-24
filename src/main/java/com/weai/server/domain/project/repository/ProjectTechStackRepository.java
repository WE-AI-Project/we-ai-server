package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ProjectTechStack;
import com.weai.server.domain.project.domain.ProjectTechStackCategory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTechStackRepository extends JpaRepository<ProjectTechStack, Long> {

	List<ProjectTechStack> findByProject_IdInOrderByProject_IdAscIdAsc(Collection<Long> projectIds);

	List<ProjectTechStack> findByProject_IdOrderByCategoryAscIdAsc(Long projectId);

	Optional<ProjectTechStack> findByProject_IdAndId(Long projectId, Long techStackId);

	boolean existsByProject_IdAndNameIgnoreCaseAndCategory(Long projectId, String name, ProjectTechStackCategory category);

	boolean existsByProject_IdAndNameIgnoreCaseAndCategoryAndIdNot(
		Long projectId,
		String name,
		ProjectTechStackCategory category,
		Long techStackId
	);
}
