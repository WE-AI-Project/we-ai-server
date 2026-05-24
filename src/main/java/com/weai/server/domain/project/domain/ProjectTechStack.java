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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "project_tech_stacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectTechStack extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 30)
	private String version;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProjectTechStackCategory category;

	@Column(name = "is_required", nullable = false)
	private boolean isRequired;

	public static ProjectTechStack create(
		Project project,
		String name,
		String version,
		ProjectTechStackCategory category,
		boolean isRequired
	) {
		return ProjectTechStack.builder()
			.project(project)
			.name(name)
			.version(version)
			.category(category)
			.isRequired(isRequired)
			.build();
	}

	public void update(
		String name,
		String version,
		ProjectTechStackCategory category,
		boolean isRequired
	) {
		this.name = name;
		this.version = version;
		this.category = category;
		this.isRequired = isRequired;
	}
}
