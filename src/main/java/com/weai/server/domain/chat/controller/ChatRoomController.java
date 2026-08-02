package com.weai.server.domain.chat.controller;

import com.weai.server.domain.chat.request.ChatMessageSendRequest;
import com.weai.server.domain.chat.response.ChatFileUploadResponse;
import com.weai.server.domain.chat.response.ChatMessageListResponse;
import com.weai.server.domain.chat.response.ChatMessageSendResponse;
import com.weai.server.domain.chat.response.ChatRoomListResponse;
import com.weai.server.domain.chat.service.ChatRoomService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

	@Operation(summary = "채팅 메시지 목록 조회", description = "로그인 사용자가 접근 가능한 채팅방의 메시지 목록을 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.CHAT_ROOM_NOT_FOUND,
		ErrorCode.CHAT_ROOM_ACCESS_DENIED,
		ErrorCode.CHAT_ROOM_NOT_ACTIVE
	})
	@GetMapping("/rooms/{chatRoomId}/messages")
	public ApiResponse<ChatMessageListResponse> getChatMessages(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long chatRoomId,
		@RequestParam(required = false) Long beforeMessageId,
		@RequestParam(required = false) Integer size,
		@RequestParam(required = false) String keyword
	) {
		return ApiResponse.success(
			"CHAT_MESSAGE_LIST_SUCCESS",
			"채팅 메시지 목록 조회에 성공했습니다.",
			chatRoomService.getChatMessages(
				authentication.getName(),
				projectId,
				chatRoomId,
				beforeMessageId,
				size,
				keyword
			)
		);
	}

	@Operation(summary = "채팅 메시지 전송", description = "로그인 사용자가 접근 가능한 채팅방에 텍스트 메시지를 전송합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.CHAT_ROOM_NOT_FOUND,
		ErrorCode.CHAT_ROOM_ACCESS_DENIED,
		ErrorCode.CHAT_ROOM_NOT_ACTIVE,
		ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED,
		ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG,
		ErrorCode.INVALID_CHAT_MESSAGE_TYPE
	})
	@PostMapping("/rooms/{chatRoomId}/messages")
	public ApiResponse<ChatMessageSendResponse> sendChatMessage(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long chatRoomId,
		@RequestBody(required = false) ChatMessageSendRequest request
	) {
		return ApiResponse.success(
			"CHAT_MESSAGE_SEND_SUCCESS",
			"채팅 메시지가 전송되었습니다.",
			chatRoomService.sendChatMessage(authentication.getName(), projectId, chatRoomId, request)
		);
	}

	@Operation(summary = "채팅 파일 업로드", description = "로그인 사용자가 접근 가능한 채팅방에 파일을 업로드하고 파일 메시지를 생성합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.CHAT_ROOM_NOT_FOUND,
		ErrorCode.CHAT_ROOM_ACCESS_DENIED,
		ErrorCode.CHAT_ROOM_NOT_ACTIVE,
		ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG,
		ErrorCode.CHAT_FILE_REQUIRED,
		ErrorCode.CHAT_FILE_EMPTY,
		ErrorCode.CHAT_FILE_SIZE_EXCEEDED,
		ErrorCode.CHAT_FILE_TYPE_NOT_ALLOWED,
		ErrorCode.CHAT_FILE_UPLOAD_FAILED
	})
	@PostMapping(value = "/rooms/{chatRoomId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<ChatFileUploadResponse> uploadChatFile(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long chatRoomId,
		@RequestParam(required = false) MultipartFile file,
		@RequestParam(required = false) String content
	) {
		return ApiResponse.success(
			"CHAT_FILE_UPLOAD_SUCCESS",
			"채팅 파일이 업로드되었습니다.",
			chatRoomService.uploadChatFile(authentication.getName(), projectId, chatRoomId, file, content)
		);
	}
}
