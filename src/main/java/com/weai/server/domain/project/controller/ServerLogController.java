package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.response.ServerLogEntryResponse;
import com.weai.server.domain.project.service.ServerLogStreamingService;
import com.weai.server.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Server Logs", description = "서버 실시간 로그 조회 및 SSE 스트리밍 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ServerLogController {

	private final ServerLogStreamingService serverLogStreamingService;

	@Operation(
		summary = "서버 실시간 로그 스트리밍 (SSE)",
		description = "서버에서 발생하는 실시간 로그를 SSE(Server-Sent Events) 스트림으로 수신합니다. 연결 시 최근 로그 버퍼('init' 이벤트)를 먼저 수신하고 이후 실시간 로그('log' 이벤트)를 수신합니다."
	)
	@GetMapping(value = "/server/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamServerLogs() {
		return serverLogStreamingService.subscribe();
	}

	@Operation(
		summary = "서버 최근 로그 목록 조회 (REST)",
		description = "메모리 버퍼에 보관 중인 서버 최근 로그 목록을 조회합니다."
	)
	@GetMapping("/server/logs")
	public ApiResponse<List<ServerLogEntryResponse>> getRecentServerLogs() {
		return ApiResponse.success(
			"SERVER_LOG_LIST_SUCCESS",
			"최근 서버 로그 목록 조회에 성공했습니다.",
			serverLogStreamingService.getRecentLogs()
		);
	}

	@Operation(
		summary = "프로젝트 서버 실시간 로그 스트리밍 (SSE)",
		description = "프로젝트 컨텍스트에서 서버 실시간 로그를 SSE 스트림으로 수신합니다."
	)
	@GetMapping(value = "/projects/{projectId}/server/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamProjectServerLogs(
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId
	) {
		return serverLogStreamingService.subscribe();
	}

	@Operation(
		summary = "프로젝트 서버 최근 로그 목록 조회 (REST)",
		description = "프로젝트 컨텍스트에서 서버 최근 로그 목록을 조회합니다."
	)
	@GetMapping("/projects/{projectId}/server/logs")
	public ApiResponse<List<ServerLogEntryResponse>> getRecentProjectServerLogs(
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId
	) {
		return ApiResponse.success(
			"PROJECT_SERVER_LOG_LIST_SUCCESS",
			"프로젝트 서버 로그 목록 조회에 성공했습니다.",
			serverLogStreamingService.getRecentLogs()
		);
	}
}
