package com.weai.server.domain.ai.backend;

import com.weai.server.domain.project.service.ProjectEnvironmentValueCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Decides which AI backend a single request should use. Precedence: the caller's personal backend
 * (if enabled) wins over the project-shared backend (if enabled), which wins over the default
 * fixed Ollama cluster. RAG retrieval (ChromaDB + our own embedding model) is unaffected by this
 * choice - only the final answer-generation model changes.
 */
@Component
@RequiredArgsConstructor
public class AiBackendResolver {

	private final UserAiBackendSettingRepository userAiBackendSettingRepository;
	private final ProjectAiBackendSettingRepository projectAiBackendSettingRepository;
	private final ProjectEnvironmentValueCipher cipher;

	public ResolvedAiBackend resolve(Long userId, Long projectId) {
		if (userId != null) {
			ResolvedAiBackend personal = userAiBackendSettingRepository.findByUser_Id(userId)
				.filter(this::isUsable)
				.map(this::toResolved)
				.orElse(null);
			if (personal != null) {
				return personal;
			}
		}

		if (projectId != null) {
			ResolvedAiBackend project = projectAiBackendSettingRepository.findByProject_Id(projectId)
				.filter(this::isUsable)
				.map(this::toResolved)
				.orElse(null);
			if (project != null) {
				return project;
			}
		}

		return ResolvedAiBackend.DEFAULT;
	}

	private boolean isUsable(UserAiBackendSetting setting) {
		return setting.isEnabled() && StringUtils.hasText(setting.getBaseUrl());
	}

	private boolean isUsable(ProjectAiBackendSetting setting) {
		return setting.isEnabled() && StringUtils.hasText(setting.getBaseUrl());
	}

	private ResolvedAiBackend toResolved(UserAiBackendSetting setting) {
		return new ResolvedAiBackend(
			ResolvedAiBackend.Source.PERSONAL,
			setting.getDialect(),
			setting.getBaseUrl(),
			setting.getModelName(),
			decrypt(setting.getEncryptedApiKey())
		);
	}

	private ResolvedAiBackend toResolved(ProjectAiBackendSetting setting) {
		return new ResolvedAiBackend(
			ResolvedAiBackend.Source.PROJECT,
			setting.getDialect(),
			setting.getBaseUrl(),
			setting.getModelName(),
			decrypt(setting.getEncryptedApiKey())
		);
	}

	private String decrypt(String encryptedApiKey) {
		return StringUtils.hasText(encryptedApiKey) ? cipher.decrypt(encryptedApiKey) : null;
	}
}
