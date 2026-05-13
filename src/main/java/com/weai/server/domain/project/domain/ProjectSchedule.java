package com.weai.server.domain.project.domain;

import com.weai.server.domain.user.domain.User;
import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "project_schedules",
	indexes = {
		@Index(name = "idx_project_schedules_project_start_date", columnList = "project_id, start_date"),
		@Index(name = "idx_project_schedules_project_status", columnList = "project_id, status")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectSchedule extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignee_id", nullable = false)
	private User assignee;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 1000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProjectDepartment department;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectSchedulePriority priority;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectScheduleStatus status;

	public static ProjectSchedule create(
		Project project,
		User assignee,
		String title,
		String description,
		ProjectDepartment department,
		LocalDate startDate,
		LocalDate endDate,
		ProjectSchedulePriority priority,
		ProjectScheduleStatus status
	) {
		return ProjectSchedule.builder()
			.project(project)
			.assignee(assignee)
			.title(title)
			.description(description)
			.department(department)
			.startDate(startDate)
			.endDate(endDate)
			.priority(priority)
			.status(status)
			.build();
	}
}
