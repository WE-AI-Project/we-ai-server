package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "빌드 태스크 실행 결과 응답")
public record BuildTaskExecutionResponse(
	@Schema(description = "실행된 태스크 이름", example = "build")
	String taskName,

	@Schema(description = "실행 명령", example = "./gradlew.bat build")
	String command,

	@Schema(description = "실행 상태 (SUCCESS, FAILED)", example = "SUCCESS")
	String status,

	@Schema(description = "프로세스 종료 코드", example = "0")
	int exitCode,

	@Schema(description = "소요 시간 (초)", example = "4.25s")
	String duration,

	@ArraySchema(schema = @Schema(description = "실행 출력 로그 라인"))
	List<String> logs,

	@Schema(description = "실행 완료 시각", example = "10:45:12")
	String executedAt
) {
}
