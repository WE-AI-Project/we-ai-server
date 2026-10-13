package com.weai.server.domain.synchub.controller;

import com.weai.server.domain.synchub.response.SyncHubRepositoryResponse;
import com.weai.server.domain.synchub.service.SyncHubHistoryService;
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
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "SyncHub 저장소", description = "프로젝트 SyncHub(서버 호스팅 Git 저장소) 정보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/git")
public class SyncHubRepositoryController {

	private final SyncHubHistoryService syncHubHistoryService;

	@Operation(
		summary = "SyncHub 저장소 정보 조회",
		description = "앱이 git remote로 등록할 SyncHub 저장소 URL과 기본 브랜치, 최신 커밋, 브랜치 목록을 조회합니다. "
			+ "저장소는 프로젝트 멤버가 처음 clone/push할 때 서버에 생성됩니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED
	})
	@GetMapping("/repository")
	public ApiResponse<SyncHubRepositoryResponse> getRepository(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"SYNCHUB_REPOSITORY_SUCCESS",
			"SyncHub 저장소 정보 조회에 성공했습니다.",
			syncHubHistoryService.getRepositoryInfo(authentication.getName(), projectId)
		);
	}
}
