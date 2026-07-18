package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import java.time.LocalDateTime;
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

	Optional<ProjectMember> findByProject_IdAndUser_IdAndStatus(Long projectId, Long userId, ProjectMemberStatus status);

	boolean existsByProject_IdAndUser_IdAndStatus(Long projectId, Long userId, ProjectMemberStatus status);

	long countByProject_IdAndStatus(Long projectId, ProjectMemberStatus status);

	long countByProject_IdAndRoleAndStatus(Long projectId, ProjectMemberRole role, ProjectMemberStatus status);

	long countByUser_IdAndStatusAndProject_Status(
		Long userId,
		ProjectMemberStatus memberStatus,
		ProjectStatus projectStatus
	);

	long countByUser_IdAndRoleAndStatusAndProject_Status(
		Long userId,
		ProjectMemberRole role,
		ProjectMemberStatus memberStatus,
		ProjectStatus projectStatus
	);

	@Query("""
		select pm
		from ProjectMember pm
		join fetch pm.user u
		where pm.project.id = :projectId
		  and pm.status = :memberStatus
		order by pm.joinedAt asc, pm.id asc
		""")
	List<ProjectMember> findByProjectIdAndStatusWithUser(
		@Param("projectId") Long projectId,
		@Param("memberStatus") ProjectMemberStatus memberStatus
	);

	@Query("""
		select pm
		from ProjectMember pm
		join fetch pm.user u
		where pm.project.id = :projectId
		  and pm.id = :projectMemberId
		""")
	Optional<ProjectMember> findByProjectIdAndIdWithUser(
		@Param("projectId") Long projectId,
		@Param("projectMemberId") Long projectMemberId
	);

	@Query("""
		select pm.project.id as projectId, count(pm.id) as memberCount
		from ProjectMember pm
		where pm.project.id in :projectIds
		  and pm.status = com.weai.server.domain.project.domain.ProjectMemberStatus.ACTIVE
		group by pm.project.id
		""")
	List<ProjectMemberCountProjection> countActiveMembersByProjectIds(@Param("projectIds") Collection<Long> projectIds);

	@Query("""
		select pm
		from ProjectMember pm
		join fetch pm.user u
		where pm.project.id = :projectId
		  and pm.status = com.weai.server.domain.project.domain.ProjectMemberStatus.ACTIVE
		  and pm.joinedAt between :startAt and :endAt
		order by pm.joinedAt desc, pm.id desc
		""")
	List<ProjectMember> findDailyStandupJoinedMembers(
		@Param("projectId") Long projectId,
		@Param("startAt") LocalDateTime startAt,
		@Param("endAt") LocalDateTime endAt
	);
}
