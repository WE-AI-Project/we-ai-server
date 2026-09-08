package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.response.ServerLogClearResponse;
import com.weai.server.domain.project.response.ServerLogListResponse;
import com.weai.server.domain.project.service.ServerLogService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Server Log", description = "프로젝트 서버 로그 조회 및 스트리밍 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/server-logs")
public class ServerLogController {

	private final ServerLogService serverLogService;

	@Operation(summary = "서버 로그 목록 조회", description = "프로젝트 서버 로그 목록을 최신순으로 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping
	public ApiResponse<ServerLogListResponse> getLogs(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "페이지 번호", example = "0") @RequestParam(required = false) Integer page,
		@Parameter(description = "페이지 크기", example = "100") @RequestParam(required = false) Integer size
	) {
		return ApiResponse.success(
			"SERVER_LOG_LIST_SUCCESS",
			"서버 로그 목록 조회에 성공했습니다.",
			serverLogService.getLogs(authentication.getName(), projectId, page, size)
		);
	}

	@Operation(summary = "서버 로그 실시간 스트리밍", description = "프로젝트 서버 로그를 SSE 방식으로 실시간 스트리밍합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.INVALID_SERVER_LOG_LEVEL,
		ErrorCode.INVALID_SERVER_LOG_SOURCE,
		ErrorCode.LOG_KEYWORD_TOO_LONG,
		ErrorCode.SERVER_LOG_STREAM_FAILED
	})
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamLogs(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "로그 레벨", example = "ERROR") @RequestParam(required = false) String level,
		@Parameter(description = "로그 발생 원본", example = "SPRING_BOOT") @RequestParam(required = false) String source,
		@Parameter(description = "로그 메시지 검색어", example = "Database") @RequestParam(required = false) String keyword
	) {
		return serverLogService.streamLogs(authentication.getName(), projectId, level, source, keyword);
	}

	@Operation(summary = "서버 로그 검색 및 필터 조회", description = "프로젝트 서버 로그를 검색어, 레벨, 기간으로 필터링합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.INVALID_SERVER_LOG_LEVEL,
		ErrorCode.INVALID_SERVER_LOG_SOURCE,
		ErrorCode.INVALID_LOG_DATE_RANGE,
		ErrorCode.LOG_KEYWORD_TOO_LONG
	})
	@GetMapping("/search")
	public ApiResponse<ServerLogListResponse> searchLogs(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "로그 메시지 검색어", example = "Database") @RequestParam(required = false) String keyword,
		@Parameter(description = "로그 레벨", example = "ERROR") @RequestParam(required = false) String level,
		@Parameter(description = "로그 발생 원본", example = "SPRING_BOOT") @RequestParam(required = false) String source,
		@Parameter(description = "조회 시작 일시", example = "2026-09-08T00:00:00")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(required = false) LocalDateTime startDateTime,
		@Parameter(description = "조회 종료 일시", example = "2026-09-08T23:59:59")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(required = false) LocalDateTime endDateTime,
		@Parameter(description = "페이지 번호", example = "0") @RequestParam(required = false) Integer page,
		@Parameter(description = "페이지 크기", example = "100") @RequestParam(required = false) Integer size
	) {
		return ApiResponse.success(
			"SERVER_LOG_SEARCH_SUCCESS",
			"서버 로그 검색 및 필터 조회에 성공했습니다.",
			serverLogService.searchLogs(
				authentication.getName(), projectId, keyword, level, source, startDateTime, endDateTime, page, size
			)
		);
	}

	@Operation(summary = "서버 로그 초기화", description = "프로젝트 서버 로그를 soft delete 방식으로 초기화합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.PROJECT_LEADER_ONLY,
		ErrorCode.SERVER_LOG_CLEAR_FAILED
	})
	@DeleteMapping
	public ApiResponse<ServerLogClearResponse> clearLogs(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId
	) {
		return ApiResponse.success(
			"SERVER_LOG_CLEAR_SUCCESS",
			"서버 로그가 초기화되었습니다.",
			serverLogService.clearLogs(authentication.getName(), projectId)
		);
	}
}
