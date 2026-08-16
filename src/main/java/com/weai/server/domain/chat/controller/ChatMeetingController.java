package com.weai.server.domain.chat.controller;

import com.weai.server.domain.chat.request.MeetingEndRequest;
import com.weai.server.domain.chat.request.MeetingStartRequest;
import com.weai.server.domain.chat.response.MeetingEndResponse;
import com.weai.server.domain.chat.response.MeetingMinuteListResponse;
import com.weai.server.domain.chat.response.MeetingStartResponse;
import com.weai.server.domain.chat.service.ChatDocumentMeetingService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chat Meeting", description = "채팅 회의 모드와 회의록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/chat/meetings")
public class ChatMeetingController {

	private final ChatDocumentMeetingService chatDocumentMeetingService;

	@Operation(summary = "회의 모드 시작", description = "프로젝트 채팅에서 회의 모드를 시작합니다. 프로젝트당 동시에 하나의 회의만 진행할 수 있습니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.CHAT_ROOM_NOT_FOUND,
		ErrorCode.MEETING_TITLE_REQUIRED,
		ErrorCode.MEETING_ALREADY_IN_PROGRESS
	})
	@PostMapping("/start")
	public ApiResponse<MeetingStartResponse> startMeeting(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@RequestBody(required = false) MeetingStartRequest request
	) {
		return ApiResponse.success(
			"MEETING_START_SUCCESS",
			"회의 모드가 시작되었습니다.",
			chatDocumentMeetingService.startMeeting(authentication.getName(), projectId, request)
		);
	}

	@Operation(summary = "회의 모드 종료 및 회의록 저장", description = "진행 중인 회의를 종료하고 회의록 원문, 요약, 액션아이템을 저장합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.MEETING_NOT_FOUND,
		ErrorCode.MEETING_ALREADY_ENDED,
		ErrorCode.MEETING_MINUTE_CONTENT_REQUIRED,
		ErrorCode.MEETING_PARTICIPANT_NOT_PROJECT_MEMBER
	})
	@PostMapping("/{meetingId}/end")
	public ApiResponse<MeetingEndResponse> endMeeting(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "회의 ID") @PathVariable Long meetingId,
		@RequestBody(required = false) MeetingEndRequest request
	) {
		return ApiResponse.success(
			"MEETING_END_AND_MINUTE_SAVE_SUCCESS",
			"회의가 종료되고 회의록이 저장되었습니다.",
			chatDocumentMeetingService.endMeeting(authentication.getName(), projectId, meetingId, request)
		);
	}

	@Operation(summary = "회의록 목록 조회", description = "프로젝트의 회의록 목록을 생성일 내림차순으로 조회합니다. 검색어와 기간 필터를 지원합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/minutes")
	public ApiResponse<MeetingMinuteListResponse> getMeetingMinutes(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
		@Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
		@Parameter(description = "검색어") @RequestParam(required = false) String keyword,
		@Parameter(description = "시작일") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@Parameter(description = "종료일") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
	) {
		return ApiResponse.success(
			"MEETING_MINUTE_LIST_SUCCESS",
			"회의록 목록 조회에 성공했습니다.",
			chatDocumentMeetingService.getMeetingMinutes(
				authentication.getName(),
				projectId,
				page,
				size,
				keyword,
				startDate,
				endDate
			)
		);
	}
}
