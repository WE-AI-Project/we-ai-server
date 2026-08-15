package com.weai.server.domain.ai.qa;

import com.weai.server.domain.ai.qa.response.CommitQaResultResponse;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "QA", description = "AI QA 실행 상태와 QA 리포트 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/commits/{commitId}/qa")
public class CommitQaQueryController {

	private final QaQueryService qaQueryService;

	@Operation(
		summary = "커밋별 QA 결과 조회",
		description = "프로젝트의 특정 커밋에 연결된 QA 리포트 목록과 최신 QA 결과를 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_PROJECT_REPOSITORY_TYPE,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.PROJECT_REPOSITORY_NOT_FOUND,
		ErrorCode.COMMIT_NOT_FOUND
	})
	@GetMapping
	public ApiResponse<CommitQaResultResponse> getCommitQaResult(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "커밋 ID 또는 커밋 해시") @PathVariable String commitId,
		@Parameter(description = "저장소 구분") @RequestParam(defaultValue = "BACKEND") String repositoryType
	) {
		return ApiResponse.success(
			"COMMIT_QA_RESULT_SUCCESS",
			"커밋별 QA 결과 조회에 성공했습니다.",
			qaQueryService.getCommitQaResult(authentication.getName(), projectId, commitId, repositoryType)
		);
	}
}
