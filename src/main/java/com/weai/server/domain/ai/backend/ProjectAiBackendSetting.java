package com.weai.server.domain.ai.backend;

import com.weai.server.domain.project.domain.Project;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "project_ai_backend_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectAiBackendSetting extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "project_ai_backend_setting_id")
	private Long projectAiBackendSettingId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false)
	private boolean enabled;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AiBackendDialect dialect;

	@Column(name = "base_url", length = 500)
	private String baseUrl;

	@Column(name = "model_name", length = 100)
	private String modelName;

	@Column(name = "health_path", nullable = false, length = 200)
	private String healthPath;

	@Column(name = "encrypted_api_key", columnDefinition = "TEXT")
	private String encryptedApiKey;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "updated_by", nullable = false)
	private User updatedBy;

	public static ProjectAiBackendSetting createDefault(Project project, User user) {
		return ProjectAiBackendSetting.builder()
			.project(project)
			.enabled(false)
			.dialect(AiBackendDialect.OLLAMA_NATIVE)
			.healthPath("/api/tags")
			.createdBy(user)
			.updatedBy(user)
			.build();
	}

	public void applySettings(
		boolean enabled, AiBackendDialect dialect, String baseUrl, String modelName, String healthPath, User updatedBy
	) {
		this.enabled = enabled;
		this.dialect = dialect;
		this.baseUrl = baseUrl;
		this.modelName = modelName;
		this.healthPath = healthPath;
		this.updatedBy = updatedBy;
	}

	public void replaceApiKey(String encryptedApiKey, User updatedBy) {
		this.encryptedApiKey = encryptedApiKey;
		this.updatedBy = updatedBy;
	}

	public void clearApiKey(User updatedBy) {
		this.encryptedApiKey = null;
		this.updatedBy = updatedBy;
	}
}
