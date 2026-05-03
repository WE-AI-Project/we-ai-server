package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	@Query("""
		select pm
		from ProjectMember pm
		join fetch pm.project p
		where pm.user.id = :userId
		  and pm.status = :memberStatus
		  and p.status = :projectStatus
		order by p.createdAt desc
		""")
	List<ProjectMember> findActiveProjectsByUserId(
		@Param("userId") Long userId,
		@Param("memberStatus") ProjectMemberStatus memberStatus,
		@Param("projectStatus") ProjectStatus projectStatus
	);

	Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);

	@Query("""
		select pm.project.id as projectId, count(pm.id) as memberCount
		from ProjectMember pm
		where pm.project.id in :projectIds
		  and pm.status = com.weai.server.domain.project.domain.ProjectMemberStatus.ACTIVE
		group by pm.project.id
		""")
	List<ProjectMemberCountProjection> countActiveMembersByProjectIds(@Param("projectIds") Collection<Long> projectIds);
}
