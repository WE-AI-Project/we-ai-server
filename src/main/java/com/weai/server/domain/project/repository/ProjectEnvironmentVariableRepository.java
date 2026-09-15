package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ProjectEnvironmentVariable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectEnvironmentVariableRepository extends JpaRepository<ProjectEnvironmentVariable, Long> {

	@Query("""
		select variable
		from ProjectEnvironmentVariable variable
		where variable.project.id = :projectId
		  and variable.deletedAt is null
		  and (:profile is null or variable.profile = :profile)
		  and (:enabled is null or variable.enabled = :enabled)
		order by variable.profile asc, variable.variableKey asc
		""")
	List<ProjectEnvironmentVariable> findActiveVariables(
		@Param("projectId") Long projectId,
		@Param("profile") String profile,
		@Param("enabled") Boolean enabled
	);

	Optional<ProjectEnvironmentVariable> findByEnvironmentVariableIdAndProject_IdAndDeletedAtIsNull(
		Long environmentVariableId,
		Long projectId
	);

	Optional<ProjectEnvironmentVariable> findByProject_IdAndProfileAndVariableKeyAndDeletedAtIsNull(
		Long projectId,
		String profile,
		String variableKey
	);

	List<ProjectEnvironmentVariable> findByProject_IdAndProfileAndEnabledTrueAndDeletedAtIsNullOrderByVariableKeyAsc(
		Long projectId,
		String profile
	);
}
