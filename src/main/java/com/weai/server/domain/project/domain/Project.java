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
	name = "projects",
	indexes = {
		@Index(name = "idx_projects_project_code", columnList = "project_code", unique = true)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Project extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_name", nullable = false, length = 50)
	private String projectName;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "project_code", nullable = false, length = 20, unique = true)
	private String projectCode;

	@Column(name = "local_path", length = 500)
	private String localPath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectStatus status;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "target_date")
	private LocalDate targetDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	public static Project create(
		String projectName,
		String description,
		String projectCode,
		String localPath,
		LocalDate startDate,
		LocalDate targetDate,
		User createdBy
	) {
		return Project.builder()
			.projectName(projectName)
			.description(description)
			.projectCode(projectCode)
			.localPath(localPath)
			.status(ProjectStatus.ACTIVE)
			.startDate(startDate)
			.targetDate(targetDate)
			.createdBy(createdBy)
			.build();
	}
}
