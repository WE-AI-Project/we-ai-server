package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "프로젝트 정보 수정 응답")
public record ProjectUpdateResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "프로젝트 이름", example = "Synaipse Project")
	String projectName,

	@Schema(description = "프로젝트 설명", example = "AI 기반 개발 협업 플랫폼")
	String description,

	@Schema(description = "프로젝트 코드", example = "WEAI2025")
	String projectCode,

	@Schema(description = "프로젝트 저장소 URL", example = "https://github.com/example/synaipse")
	String repositoryUrl,

	@Schema(description = "프로젝트 로컬 경로", example = "D:\\Synaipse")
	String localPath,

	@Schema(description = "프로젝트 상태", example = "ACTIVE")
	ProjectStatus status,

	@Schema(description = "프로젝트 시작일", example = "2026-05-01")
	LocalDate startDate,

	@Schema(description = "프로젝트 목표일", example = "2026-06-30")
	LocalDate targetDate,

	@Schema(description = "수정 시각", example = "2026-05-25T10:00:00")
	LocalDateTime updatedAt
) {

	public static ProjectUpdateResponse from(Project project) {
		return new ProjectUpdateResponse(
			project.getId(),
			project.getProjectName(),
			project.getDescription(),
			project.getProjectCode(),
			project.getRepositoryUrl(),
			project.getLocalPath(),
			project.getStatus(),
			project.getStartDate(),
			project.getTargetDate(),
			project.getUpdatedAt()
		);
	}
}
