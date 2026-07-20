package com.weai.server.domain.chat.controller;

import com.weai.server.domain.chat.response.ChatRoomListResponse;
import com.weai.server.domain.chat.service.ChatRoomService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "채팅", description = "프로젝트 채팅 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/chat")
public class ChatRoomController {

	private final ChatRoomService chatRoomService;

	@Operation(summary = "채팅방 목록 조회", description = "로그인 사용자가 참여 중인 프로젝트의 채팅방 목록을 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.INVALID_CHAT_ROOM_TYPE,
		ErrorCode.INVALID_DEPARTMENT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/rooms")
	public ApiResponse<ChatRoomListResponse> getChatRooms(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(required = false) String type,
		@RequestParam(required = false) String department,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size
	) {
		return ApiResponse.success(
			"CHAT_ROOM_LIST_SUCCESS",
			"채팅방 목록 조회에 성공했습니다.",
			chatRoomService.getChatRooms(authentication.getName(), projectId, type, department, keyword, page, size)
		);
	}
}
