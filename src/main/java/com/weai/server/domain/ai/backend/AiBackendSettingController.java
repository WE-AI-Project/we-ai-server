package com.weai.server.domain.ai.backend;

import com.weai.server.domain.ai.support.AiRateLimiterService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Backend", description = "개인/프로젝트 공용 커스텀 AI 백엔드(자체 GPU, 타 API 키) 설정 API. 미설정 시 기본 Ollama 클러스터를 그대로 사용합니다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/backend")
public class AiBackendSettingController {

	private static final int MAX_TEST_CALLS_PER_MINUTE = 10;

	private final AiBackendSettingService aiBackendSettingService;
	private final AiRateLimiterService aiRateLimiterService;

	@Operation(summary = "개인 AI 백엔드 설정 조회")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED})
	@GetMapping("/personal")
	public ApiResponse<AiBackendSettingResponse> getPersonal(Authentication authentication) {
		return ApiResponse.success(
			"AI_BACKEND_PERSONAL_GET_SUCCESS", "개인 AI 백엔드 설정을 조회했습니다.",
			aiBackendSettingService.getPersonal(authentication.getName())
		);
	}

	@Operation(summary = "개인 AI 백엔드 설정 저장", description = "apiKey를 비워두면 기존에 저장된 키가 유지됩니다.")
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED, ErrorCode.AI_BACKEND_BASE_URL_REQUIRED, ErrorCode.AI_BACKEND_BASE_URL_NOT_ALLOWED,
		ErrorCode.AI_BACKEND_INVALID_DIALECT, ErrorCode.AI_BACKEND_ENCRYPT_FAILED
	})
	@PutMapping("/personal")
	public ApiResponse<AiBackendSettingResponse> updatePersonal(
		Authentication authentication,
		@RequestBody(required = false) AiBackendSettingUpdateRequest request
	) {
		return ApiResponse.success(
			"AI_BACKEND_PERSONAL_UPDATE_SUCCESS", "개인 AI 백엔드 설정을 저장했습니다.",
			aiBackendSettingService.updatePersonal(authentication.getName(), request)
		);
	}

	@Operation(summary = "개인 AI 백엔드 API 키 삭제")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED})
	@DeleteMapping("/personal/api-key")
	public ApiResponse<Void> deletePersonalApiKey(Authentication authentication) {
		aiBackendSettingService.deletePersonalApiKey(authentication.getName());
		return ApiResponse.successMessage("AI_BACKEND_PERSONAL_API_KEY_DELETE_SUCCESS", "저장된 API 키를 삭제했습니다.");
	}

	@Operation(summary = "프로젝트 공용 AI 백엔드 설정 조회")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED})
	@GetMapping("/projects/{projectId}")
	public ApiResponse<AiBackendSettingResponse> getProject(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId
	) {
		return ApiResponse.success(
			"AI_BACKEND_PROJECT_GET_SUCCESS", "프로젝트 공용 AI 백엔드 설정을 조회했습니다.",
			aiBackendSettingService.getProject(authentication.getName(), projectId)
		);
	}

	@Operation(
		summary = "프로젝트 공용 AI 백엔드 설정 저장",
		description = "프로젝트 LEADER만 요청할 수 있습니다. apiKey를 비워두면 기존에 저장된 키가 유지됩니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.PROJECT_LEADER_ONLY,
		ErrorCode.AI_BACKEND_BASE_URL_REQUIRED, ErrorCode.AI_BACKEND_BASE_URL_NOT_ALLOWED,
		ErrorCode.AI_BACKEND_INVALID_DIALECT, ErrorCode.AI_BACKEND_ENCRYPT_FAILED
	})
	@PutMapping("/projects/{projectId}")
	public ApiResponse<AiBackendSettingResponse> updateProject(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@RequestBody(required = false) AiBackendSettingUpdateRequest request
	) {
		return ApiResponse.success(
			"AI_BACKEND_PROJECT_UPDATE_SUCCESS", "프로젝트 공용 AI 백엔드 설정을 저장했습니다.",
			aiBackendSettingService.updateProject(authentication.getName(), projectId, request)
		);
	}

	@Operation(summary = "프로젝트 공용 AI 백엔드 API 키 삭제", description = "프로젝트 LEADER만 요청할 수 있습니다.")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.PROJECT_LEADER_ONLY})
	@DeleteMapping("/projects/{projectId}/api-key")
	public ApiResponse<Void> deleteProjectApiKey(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId
	) {
		aiBackendSettingService.deleteProjectApiKey(authentication.getName(), projectId);
		return ApiResponse.successMessage("AI_BACKEND_PROJECT_API_KEY_DELETE_SUCCESS", "저장된 API 키를 삭제했습니다.");
	}

	@Operation(summary = "커스텀 AI 백엔드 연결 테스트", description = "저장 전 후보 설정으로 서버가 대신 연결을 시도합니다 (CORS 제약 없음, 키가 클라이언트 밖으로 나가지 않음). 사설/내부 네트워크 주소는 거부됩니다.")
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.TOO_MANY_REQUESTS})
	@PostMapping("/test")
	public ApiResponse<AiBackendConnectionTestResponse> testConnection(
		Authentication authentication,
		@Valid @RequestBody AiBackendConnectionTestRequest request
	) {
		aiRateLimiterService.checkAndConsume(
			"ai-backend-test:" + authentication.getName(),
			MAX_TEST_CALLS_PER_MINUTE,
			Duration.ofMinutes(1).toMillis()
		);
		return ApiResponse.success(
			"AI_BACKEND_TEST_SUCCESS", "연결 테스트를 완료했습니다.",
			aiBackendSettingService.testConnection(request)
		);
	}
}
