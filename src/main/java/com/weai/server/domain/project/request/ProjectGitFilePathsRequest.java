package com.weai.server.domain.project.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Git 파일 경로 목록 요청")
public record ProjectGitFilePathsRequest(
	@ArraySchema(
		schema = @Schema(
			description = "프로젝트 저장소 내부의 상대 파일 경로",
			example = "src/main/java/com/weai/server/domain/project/service/ProjectGitService.java"
		)
	)
	List<String> filePaths
) {
}
