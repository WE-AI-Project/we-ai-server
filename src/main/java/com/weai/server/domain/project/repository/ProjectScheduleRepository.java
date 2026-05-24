package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectSchedule;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectScheduleRepository extends JpaRepository<ProjectSchedule, Long> {

	@Query("""
		select ps
		from ProjectSchedule ps
		join fetch ps.assignee a
		where ps.project.id = :projectId
		  and (:department is null or ps.department = :department)
		  and (:status is null or ps.status = :status)
		  and (:startDate is null or ps.startDate >= :startDate)
		  and (:endDate is null or ps.endDate <= :endDate)
		order by ps.startDate asc, ps.id asc
		""")
	List<ProjectSchedule> findByProjectIdWithFilters(
		@Param("projectId") Long projectId,
		@Param("department") ProjectDepartment department,
		@Param("status") ProjectScheduleStatus status,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);

	@Query("""
		select ps
		from ProjectSchedule ps
		join fetch ps.project p
		join fetch ps.assignee a
		where ps.project.id = :projectId
		  and ps.id = :scheduleId
		""")
	java.util.Optional<ProjectSchedule> findByProjectIdAndScheduleId(
		@Param("projectId") Long projectId,
		@Param("scheduleId") Long scheduleId
	);

	List<ProjectSchedule> findByProject_IdOrderByStartDateAscIdAsc(Long projectId);
}
