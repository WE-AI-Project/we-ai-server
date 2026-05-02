package com.weai.server.domain.user.controller;

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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
