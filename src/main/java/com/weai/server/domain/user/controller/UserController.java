package com.weai.server.domain.user.controller;

import com.weai.server.domain.user.request.UserProfileUpdateRequest;
import com.weai.server.domain.user.response.UserActivitySummaryResponse;
import com.weai.server.domain.user.response.UserRecentActivityListResponse;
import com.weai.server.domain.user.response.UserResponse;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "사용자", description = "로그인한 사용자 정보 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "현재 사용자 정보를 성공적으로 조회했습니다."
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "사용자 권한이 없는 토큰입니다.", content = @Content)
	})
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.RESOURCE_NOT_FOUND})
	@GetMapping("/me")
	public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
		return ApiResponse.success(userService.findByEmail(authentication.getName()));
	}

	@Operation(summary = "내 프로필 수정", description = "현재 로그인한 사용자의 username, name을 수정합니다.")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT, ErrorCode.CONFLICT, ErrorCode.RESOURCE_NOT_FOUND})
	@PatchMapping("/me/profile")
	public ApiResponse<UserResponse> updateMyProfile(
		Authentication authentication,
		@Valid @RequestBody UserProfileUpdateRequest request
	) {
		return ApiResponse.success(
			"USER_PROFILE_UPDATE_SUCCESS",
			"내 프로필 수정에 성공했습니다.",
			userService.updateMyProfile(authentication.getName(), request)
		);
	}

	@Operation(summary = "내 활동 요약 조회", description = "현재 로그인한 사용자의 활성 프로젝트와 담당 일정 요약을 조회합니다.")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.RESOURCE_NOT_FOUND})
	@GetMapping("/me/activity-summary")
	public ApiResponse<UserActivitySummaryResponse> getMyActivitySummary(Authentication authentication) {
		return ApiResponse.success(
			"USER_ACTIVITY_SUMMARY_SUCCESS",
			"내 활동 요약 조회에 성공했습니다.",
			userService.getMyActivitySummary(authentication.getName())
		);
	}

	@Operation(summary = "내 최근 활동 조회", description = "현재 로그인한 사용자의 프로젝트 참여, 프로젝트 생성, 담당 일정 활동을 최신순으로 조회합니다.")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT, ErrorCode.RESOURCE_NOT_FOUND})
	@GetMapping("/me/recent-activities")
	public ApiResponse<UserRecentActivityListResponse> getMyRecentActivities(
		Authentication authentication,
		@RequestParam(required = false) Integer limit
	) {
		return ApiResponse.success(
			"USER_RECENT_ACTIVITY_LIST_SUCCESS",
			"내 최근 활동 조회에 성공했습니다.",
			userService.getMyRecentActivities(authentication.getName(), limit)
		);
	}
}
