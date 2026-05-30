package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "프로젝트 정보 수정 요청")
public record ProjectUpdateRequest(
	@Schema(description = "프로젝트 이름", example = "Synaipse Project")
	String projectName,

	@Schema(description = "프로젝트 설명", example = "AI 기반 개발 협업 플랫폼")
	String description,

	@Schema(description = "프로젝트 저장소 URL", example = "https://github.com/example/synaipse")
	String repositoryUrl,

	@Schema(description = "프로젝트 로컬 경로", example = "D:\\Synaipse")
	String localPath,

	@Schema(description = "프로젝트 시작일", example = "2026-05-01")
	LocalDate startDate,

	@Schema(description = "프로젝트 목표일", example = "2026-06-30")
	LocalDate targetDate,

	@Schema(description = "프로젝트 상태", example = "ACTIVE")
	String status
) {
}
