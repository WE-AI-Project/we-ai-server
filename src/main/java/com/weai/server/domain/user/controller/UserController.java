package com.weai.server.domain.user.controller;

import com.weai.server.domain.user.dto.CreateUserRequest;
import com.weai.server.domain.user.dto.UserResponse;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "User", description = "사용자 샘플 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@Operation(summary = "사용자 목록 조회", description = "현재 등록된 샘플 사용자를 전체 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				mediaType = "application/json",
				array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))
			)
		)
	})
	@GetMapping
	public ApiResponse<List<UserResponse>> getUsers() {
		return ApiResponse.success(userService.findAll());
	}

	@Operation(summary = "사용자 단건 조회", description = "사용자 ID로 단일 사용자를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@GetMapping("/{userId}")
	public ApiResponse<UserResponse> getUser(
		@Parameter(description = "조회할 사용자 ID", example = "1")
		@PathVariable @Positive(message = "userId는 1 이상의 숫자여야 합니다.") Long userId
	) {
		return ApiResponse.success(userService.findById(userId));
	}

	@Operation(
		summary = "사용자 생성",
		description = "샘플 사용자를 생성합니다. 생성 후 Swagger에서 다시 조회해볼 수 있습니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "201",
			description = "생성 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "create-user-success",
					value = """
						{
						  "success": true,
						  "code": "SUCCESS",
						  "message": "요청이 성공적으로 처리되었습니다.",
						  "data": {
						    "id": 2,
						    "name": "김코딩",
						    "email": "coding@example.com"
						  },
						  "timestamp": "2026-03-28T20:00:00"
						}
						"""
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success(userService.create(request)));
	}
}
