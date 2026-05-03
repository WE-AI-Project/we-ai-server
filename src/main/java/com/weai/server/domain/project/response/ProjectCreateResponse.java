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

@Schema(description = "프로젝트 생성 응답")
public record ProjectCreateResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "프로젝트명", example = "WE&AI Enterprise")
	String projectName,

	@Schema(description = "자동 생성된 8자리 참여 코드", example = "D0DZ26Q4")
	String projectCode,

	@Schema(description = "프로젝트 저장 위치", example = "D:\\WE_AI\\enterprise")
	String localPath,

	@Schema(description = "프로젝트 마감일", example = "2026-05-15")
	LocalDate deadlineDate,

	@Schema(description = "오늘 기준 마감일까지 남은 일수. 마감일이 없으면 null 입니다.", example = "12")
	Integer daysRemaining,

	@Schema(description = "저장된 기술 스택 개수", example = "6")
	int techStackCount,

	@ArraySchema(schema = @Schema(description = "저장된 기술 스택 이름", example = "Spring Boot"))
	List<String> techStacks,

	@Schema(description = "생성자의 프로젝트 역할", example = "LEADER")
	ProjectMemberRole role,

	@Schema(description = "생성자의 프로젝트 부서", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "프로젝트 상태", example = "ACTIVE")
	ProjectStatus status,

	@Schema(description = "생성 시각", example = "2026-05-03T10:30:00")
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
