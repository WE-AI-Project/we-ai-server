package com.weai.server.domain.project.domain;

import com.weai.server.domain.user.domain.User;
import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "project_environment_variables",
	indexes = {
		@Index(name = "idx_project_environment_variables_project_profile", columnList = "project_id, profile"),
		@Index(name = "idx_project_environment_variables_active", columnList = "project_id, profile, enabled, deleted_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectEnvironmentVariable extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "environment_variable_id")
	private Long environmentVariableId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false, length = 30)
	private String profile;

	@Column(name = "variable_key", nullable = false, length = 100)
	private String variableKey;

	@Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
	private String encryptedValue;

	@Column(nullable = false)
	private boolean secret;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private boolean enabled;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "updated_by", nullable = false)
	private User updatedBy;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static ProjectEnvironmentVariable create(
		Project project,
		String profile,
		String variableKey,
		String encryptedValue,
		boolean secret,
		String description,
		boolean enabled,
		User user
	) {
		return ProjectEnvironmentVariable.builder()
			.project(project)
			.profile(profile)
			.variableKey(variableKey)
			.encryptedValue(encryptedValue)
			.secret(secret)
			.description(description)
			.enabled(enabled)
			.createdBy(user)
			.updatedBy(user)
			.build();
	}

	public void update(
		String profile,
		String encryptedValue,
		boolean secret,
		String description,
		boolean enabled,
		User user
	) {
		this.profile = profile;
		this.encryptedValue = encryptedValue;
		this.secret = secret;
		this.description = description;
		this.enabled = enabled;
		this.updatedBy = user;
	}

	public void delete(User user) {
		this.updatedBy = user;
		this.deletedAt = LocalDateTime.now();
	}
}
