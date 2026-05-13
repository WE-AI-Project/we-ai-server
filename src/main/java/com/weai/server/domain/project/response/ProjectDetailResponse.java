package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Project detail response")
public record ProjectDetailResponse(
	@Schema(description = "Project id", example = "1")
	Long projectId,

	@Schema(description = "Project name", example = "WE&AI Enterprise")
	String projectName,

	@Schema(description = "Project description", example = "AI-based developer collaboration platform")
	String description,

	@Schema(description = "Project join code", example = "WEAI2025")
	String projectCode,

	@Schema(description = "Repository URL", example = "https://github.com/example/we-ai")
	String repositoryUrl,

	@Schema(description = "Local working path", example = "D:\\WE_AI\\enterprise")
	String localPath,

	@Schema(description = "Project status", example = "ACTIVE")
	ProjectStatus status,

	@Schema(description = "Project start date", example = "2026-05-01")
	LocalDate startDate,

	@Schema(description = "Project target date", example = "2026-06-30")
	LocalDate targetDate,

	@Schema(description = "Created at", example = "2026-05-01T10:00:00")
	LocalDateTime createdAt,

	@Schema(description = "Updated at", example = "2026-05-02T12:00:00")
	LocalDateTime updatedAt
) {

	public static ProjectDetailResponse from(Project project) {
		return new ProjectDetailResponse(
			project.getId(),
			project.getProjectName(),
			project.getDescription(),
			project.getProjectCode(),
			project.getRepositoryUrl(),
			project.getLocalPath(),
			project.getStatus(),
			project.getStartDate(),
			project.getTargetDate(),
			project.getCreatedAt(),
			project.getUpdatedAt()
		);
	}
}
