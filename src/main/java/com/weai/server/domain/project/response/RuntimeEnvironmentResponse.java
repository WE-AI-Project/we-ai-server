package com.weai.server.domain.project.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "런타임 환경 정보 조회 응답")
public record RuntimeEnvironmentResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "프로젝트 이름", example = "Synaipse Backend")
	String projectName,

	@Schema(description = "Java 버전", example = "17.0.12")
	String javaVersion,

	@Schema(description = "JVM 이름", example = "OpenJDK 64-Bit Server VM")
	String jvmName,

	@Schema(description = "JVM 공급자", example = "Eclipse Adoptium")
	String jvmVendor,

	@Schema(description = "운영체제 이름", example = "Windows 10")
	String osName,

	@Schema(description = "운영체제 버전", example = "10.0")
	String osVersion,

	@Schema(description = "운영체제 아키텍처", example = "amd64")
	String osArch,

	@Schema(description = "사용 가능한 프로세서 수", example = "8")
	int availableProcessors,

	@Schema(description = "서버 타임존", example = "Asia/Seoul")
	String timezone,

	@Schema(description = "서버 현재 시간", example = "2026-09-13T13:50:00")
	LocalDateTime serverTime,

	@Schema(description = "Spring Boot 버전", example = "3.3.0")
	String springBootVersion,

	@ArraySchema(schema = @Schema(description = "현재 서버 Active profile", example = "dev"))
	List<String> activeProfiles,

	@ArraySchema(schema = @Schema(description = "현재 서버 Default profile", example = "default"))
	List<String> defaultProfiles,

	@Schema(description = "JVM 메모리 정보")
	MemoryResponse memory
) {

	@Schema(description = "JVM 메모리 정보")
	public record MemoryResponse(
		@Schema(description = "최대 메모리", example = "4294967296")
		long maxMemory,

		@Schema(description = "총 메모리", example = "536870912")
		long totalMemory,

		@Schema(description = "사용 가능한 메모리", example = "256000000")
		long freeMemory,

		@Schema(description = "사용 중인 메모리", example = "280870912")
		long usedMemory
	) {
	}
}
