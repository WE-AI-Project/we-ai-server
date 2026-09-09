package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.LibraryResourceCategory;
import com.weai.server.domain.project.domain.ProjectLibraryResource;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectLibraryResourceRepository extends JpaRepository<ProjectLibraryResource, Long> {

	Page<ProjectLibraryResource> findByProject_IdAndDeletedAtIsNull(Long projectId, Pageable pageable);

	Page<ProjectLibraryResource> findByProject_IdAndCategoryAndDeletedAtIsNull(
		Long projectId, LibraryResourceCategory category, Pageable pageable);

	Optional<ProjectLibraryResource> findByIdAndProject_IdAndDeletedAtIsNull(Long id, Long projectId);

	long countByProject_IdAndDeletedAtIsNull(Long projectId);

	long countByProject_IdAndCategoryAndDeletedAtIsNull(Long projectId, LibraryResourceCategory category);
}
