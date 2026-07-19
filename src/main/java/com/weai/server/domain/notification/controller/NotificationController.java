package com.weai.server.domain.notification.controller;

import com.weai.server.domain.notification.domain.NotificationType;
import com.weai.server.domain.notification.response.NotificationDeleteResponse;
import com.weai.server.domain.notification.response.NotificationListResponse;
import com.weai.server.domain.notification.response.NotificationReadAllResponse;
import com.weai.server.domain.notification.response.NotificationReadResponse;
import com.weai.server.domain.notification.service.NotificationService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "알림", description = "프로젝트 알림 패널 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	@Operation(summary = "내 알림 목록 조회", description = "로그인 사용자의 프로젝트 알림 목록을 조회합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping
	public ApiResponse<NotificationListResponse> getMyNotifications(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size,
		@RequestParam(required = false) NotificationType type,
		@RequestParam(required = false) Boolean isRead
	) {
		return ApiResponse.success(
			"NOTIFICATION_LIST_SUCCESS",
			"알림 목록 조회에 성공했습니다.",
			notificationService.getMyNotifications(authentication.getName(), projectId, page, size, type, isRead)
		);
	}

	@Operation(summary = "알림 읽음 처리", description = "로그인 사용자의 특정 알림을 읽음 처리합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.NOTIFICATION_NOT_FOUND,
		ErrorCode.NOTIFICATION_READ_FAILED
	})
	@PatchMapping("/{notificationId}/read")
	public ApiResponse<NotificationReadResponse> readNotification(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long notificationId
	) {
		return ApiResponse.success(
			"NOTIFICATION_READ_SUCCESS",
			"알림을 읽음 처리했습니다.",
			notificationService.readNotification(authentication.getName(), projectId, notificationId)
		);
	}

	@Operation(summary = "전체 알림 읽음 처리", description = "로그인 사용자의 프로젝트 내 모든 알림을 읽음 처리합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.NOTIFICATION_READ_FAILED
	})
	@PatchMapping("/read-all")
	public ApiResponse<NotificationReadAllResponse> readAllNotifications(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"NOTIFICATION_READ_ALL_SUCCESS",
			"전체 알림을 읽음 처리했습니다.",
			notificationService.readAllNotifications(authentication.getName(), projectId)
		);
	}

	@Operation(summary = "알림 삭제", description = "로그인 사용자의 특정 알림을 삭제합니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.NOTIFICATION_NOT_FOUND,
		ErrorCode.NOTIFICATION_DELETE_FAILED
	})
	@DeleteMapping("/{notificationId}")
	public ApiResponse<NotificationDeleteResponse> deleteNotification(
		Authentication authentication,
		@PathVariable Long projectId,
		@PathVariable Long notificationId
	) {
		return ApiResponse.success(
			"NOTIFICATION_DELETE_SUCCESS",
			"알림이 삭제되었습니다.",
			notificationService.deleteNotification(authentication.getName(), projectId, notificationId)
		);
	}
}
