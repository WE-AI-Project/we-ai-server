package com.weai.server.domain.project.response;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Schema(description = "Project create response")
public record ProjectCreateResponse(
	@Schema(description = "Project id", example = "1")
	Long projectId,

	@Schema(description = "Project name", example = "WE&AI Enterprise")
	String projectName,

	@Schema(description = "Generated join code", example = "D0DZ26Q4")
	String projectCode,

	@Schema(description = "Repository URL", example = "https://github.com/example/we-ai")
	String repositoryUrl,

	@Schema(description = "Local working path", example = "D:\\WE_AI\\enterprise")
	String localPath,

	@Schema(description = "Project deadline", example = "2026-05-15")
	LocalDate deadlineDate,

	@Schema(description = "Days remaining until deadline. Null when no deadline is set.", example = "12")
	Integer daysRemaining,

	@Schema(description = "Registered tech stack count", example = "6")
	int techStackCount,

	@ArraySchema(schema = @Schema(description = "Registered tech stack name", example = "Spring Boot"))
	List<String> techStacks,

	@Schema(description = "Creator role in the project", example = "LEADER")
	ProjectMemberRole role,

	@Schema(description = "Creator department in the project", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "Project status", example = "ACTIVE")
	ProjectStatus status,

	@Schema(description = "Created at", example = "2026-05-03T10:30:00")
	LocalDateTime createdAt
) {

	public static ProjectCreateResponse from(
		Project project,
		ProjectMember projectMember,
		List<String> techStacks,
		LocalDate today
	) {
		return new ProjectCreateResponse(
			project.getId(),
			project.getProjectName(),
			project.getProjectCode(),
			project.getRepositoryUrl(),
			project.getLocalPath(),
			project.getTargetDate(),
			calculateDaysRemaining(today, project.getTargetDate()),
			techStacks.size(),
			techStacks,
			projectMember.getRole(),
			projectMember.getDepartment(),
			project.getStatus(),
			project.getCreatedAt()
		);
	}

	private static Integer calculateDaysRemaining(LocalDate today, LocalDate deadlineDate) {
		if (deadlineDate == null) {
			return null;
		}

		return Math.toIntExact(ChronoUnit.DAYS.between(today, deadlineDate));
	}
}
