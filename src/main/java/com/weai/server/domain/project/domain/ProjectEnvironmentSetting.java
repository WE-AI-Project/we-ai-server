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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "project_environment_settings",
	indexes = {
		@Index(name = "idx_project_environment_settings_project", columnList = "project_id", unique = true)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectEnvironmentSetting extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "environment_setting_id")
	private Long environmentSettingId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "active_profile", nullable = false, length = 30)
	private String activeProfile;

	@Enumerated(EnumType.STRING)
	@Column(name = "build_tool", nullable = false, length = 30)
	private BuildTool buildTool;

	@Column(name = "java_version", length = 50)
	private String javaVersion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "changed_by")
	private User changedBy;

	@Column(name = "changed_at")
	private LocalDateTime changedAt;

	public static ProjectEnvironmentSetting create(
		Project project,
		String activeProfile,
		BuildTool buildTool,
		String javaVersion,
		User changedBy
	) {
		LocalDateTime now = LocalDateTime.now();
		return ProjectEnvironmentSetting.builder()
			.project(project)
			.activeProfile(activeProfile)
			.buildTool(buildTool)
			.javaVersion(javaVersion)
			.changedBy(changedBy)
			.changedAt(now)
			.build();
	}

	public void updateActiveProfile(String activeProfile, User changedBy) {
		this.activeProfile = activeProfile;
		this.changedBy = changedBy;
		this.changedAt = LocalDateTime.now();
	}
}
