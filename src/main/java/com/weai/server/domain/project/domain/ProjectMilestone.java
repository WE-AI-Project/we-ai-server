package com.weai.server.domain.project.domain;

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
	name = "project_milestones",
	indexes = {
		@Index(name = "idx_project_milestones_project_start_date", columnList = "project_id, start_date"),
		@Index(name = "idx_project_milestones_project_status", columnList = "project_id, status")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectMilestone extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "milestone_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 1000)
	private String description;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectMilestoneStatus status;

	@Column(name = "progress_rate", nullable = false)
	private int progressRate;

	public static ProjectMilestone create(
		Project project,
		String title,
		String description,
		LocalDate startDate,
		LocalDate endDate,
		ProjectMilestoneStatus status,
		int progressRate
	) {
		return ProjectMilestone.builder()
			.project(project)
			.title(title)
			.description(description)
			.startDate(startDate)
			.endDate(endDate)
			.status(status)
			.progressRate(progressRate)
			.build();
	}
}
