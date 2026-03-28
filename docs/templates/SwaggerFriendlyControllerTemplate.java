package com.weai.server.domain.sample.controller;

import com.weai.server.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * 사용 방법
 * 1. 이 파일을 src/main/java 아래 원하는 패키지로 복사합니다.
 * 2. package, 클래스명, URL 경로(/api/v1/users), DTO 이름을 프로젝트에 맞게 바꿉니다.
 * 3. 예시로 들어간 하드코딩 응답을 실제 Service 호출로 교체합니다.
 */

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/v1/users")
public class SwaggerFriendlyControllerTemplate {

	@Operation(
		summary = "사용자 단건 조회",
		description = "사용자 ID로 단일 사용자를 조회합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiResponse.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@GetMapping("/{userId}")
	public ApiResponse<UserResponse> getUser(
		@Parameter(description = "조회할 사용자 ID", example = "1")
		@PathVariable Long userId
	) {
		UserResponse response = new UserResponse(userId, "홍길동", "gildong@example.com");
		return ApiResponse.success(response);
	}

	@Operation(
		summary = "사용자 생성",
		description = "새로운 사용자를 생성합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "201",
			description = "생성 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "success",
					value = """
						{
						  "success": true,
						  "code": "SUCCESS",
						  "message": "요청이 성공적으로 처리되었습니다.",
						  "data": {
						    "id": 1,
						    "name": "홍길동",
						    "email": "gildong@example.com"
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
	public ResponseEntity<ApiResponse<UserResponse>> createUser(
		@Valid @RequestBody CreateUserRequest request
	) {
		UserResponse response = new UserResponse(1L, request.name(), request.email());
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	@Schema(description = "사용자 생성 요청 DTO")
	public record CreateUserRequest(
		@Schema(description = "사용자 이름", example = "홍길동")
		@NotBlank(message = "이름은 필수입니다.")
		@Size(max = 20, message = "이름은 20자 이하여야 합니다.")
		String name,

		@Schema(description = "사용자 이메일", example = "gildong@example.com")
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "올바른 이메일 형식이어야 합니다.")
		String email
	) {
	}

	@Schema(description = "사용자 응답 DTO")
	public record UserResponse(
		@Schema(description = "사용자 ID", example = "1")
		Long id,

		@Schema(description = "사용자 이름", example = "홍길동")
		String name,

		@Schema(description = "사용자 이메일", example = "gildong@example.com")
		String email
	) {
	}
}
