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

@Schema(description = "내 프로젝트 목록 응답 항목")
public record MyProjectResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "프로젝트명", example = "WE&AI Backend Server")
	String projectName,

	@Schema(description = "프로젝트 설명", example = "AI 기반 개발 협업 플랫폼 백엔드")
	String description,

	@Schema(description = "프로젝트 참여 코드", example = "WEAI2025")
	String projectCode,

	@Schema(description = "내 역할", example = "LEADER")
	ProjectMemberRole role,

	@Schema(description = "내 부서", example = "BACKEND")
	ProjectDepartment department,

	@Schema(description = "프로젝트 상태", example = "ACTIVE")
	ProjectStatus status,

	@ArraySchema(schema = @Schema(description = "저장된 기술 스택 이름", example = "Spring Boot"))
	List<String> techStacks,

	@Schema(description = "프로젝트 마감일", example = "2026-06-30")
	LocalDate deadlineDate,

	@Schema(description = "오늘 기준 마감일까지 남은 일수. 마감일이 없으면 null 입니다.", example = "58")
	Integer daysRemaining,

	@Schema(description = "현재 활성 멤버 수", example = "6")
	long memberCount,

	@Schema(description = "생성 시각", example = "2026-05-03T10:00:00")
	LocalDateTime createdAt
) {

	public static MyProjectResponse from(
		ProjectMember projectMember,
		List<String> techStacks,
		long memberCount,
		LocalDate today
	) {
		Project project = projectMember.getProject();
		return new MyProjectResponse(
			project.getId(),
			project.getProjectName(),
			project.getDescription(),
			project.getProjectCode(),
			projectMember.getRole(),
			projectMember.getDepartment(),
			project.getStatus(),
			techStacks,
			project.getTargetDate(),
			calculateDaysRemaining(today, project.getTargetDate()),
			memberCount,
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
