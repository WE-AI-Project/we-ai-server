package com.weai.server.domain.ai.qa;

import com.weai.server.domain.ai.qa.response.QaReportDetailResponse;
import com.weai.server.domain.ai.qa.response.QaReportListResponse;
import com.weai.server.domain.ai.qa.response.QaRunStatusResponse;
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
@RequestMapping("/api/v1/projects/{projectId}/qa")
public class ProjectQaQueryController {

	private final QaQueryService qaQueryService;

	@Operation(
		summary = "AI QA 실행 상태 조회",
		description = "프로젝트의 특정 AI QA 실행 건에 대한 현재 상태와 진행 정보를 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.QA_RUN_NOT_FOUND
	})
	@GetMapping("/runs/{qaRunId}/status")
	public ApiResponse<QaRunStatusResponse> getQaRunStatus(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "QA 실행 ID") @PathVariable Long qaRunId
	) {
		return ApiResponse.success(
			"QA_RUN_STATUS_SUCCESS",
			"AI QA 실행 상태 조회에 성공했습니다.",
			qaQueryService.getQaRunStatus(authentication.getName(), projectId, qaRunId)
		);
	}

	@Operation(
		summary = "QA 리포트 목록 조회",
		description = "프로젝트의 QA 리포트 목록을 생성일 내림차순으로 조회합니다. status와 commitId 필터를 선택적으로 사용할 수 있습니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.INVALID_INPUT,
		ErrorCode.INVALID_QA_REPORT_STATUS,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED
	})
	@GetMapping("/reports")
	public ApiResponse<QaReportListResponse> getQaReports(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") Integer page,
		@Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") Integer size,
		@Parameter(description = "QA 리포트 상태") @RequestParam(required = false) String status,
		@Parameter(description = "커밋 ID 또는 커밋 해시") @RequestParam(required = false) String commitId
	) {
		return ApiResponse.success(
			"QA_REPORT_LIST_SUCCESS",
			"QA 리포트 목록 조회에 성공했습니다.",
			qaQueryService.getQaReports(authentication.getName(), projectId, page, size, status, commitId)
		);
	}

	@Operation(
		summary = "QA 리포트 상세 조회",
		description = "프로젝트의 특정 QA 리포트 상세 내용, 이슈 목록, 테스트 결과를 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.QA_REPORT_NOT_FOUND
	})
	@GetMapping("/reports/{qaReportId}")
	public ApiResponse<QaReportDetailResponse> getQaReportDetail(
		Authentication authentication,
		@Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
		@Parameter(description = "QA 리포트 ID") @PathVariable Long qaReportId
	) {
		return ApiResponse.success(
			"QA_REPORT_DETAIL_SUCCESS",
			"QA 리포트 상세 조회에 성공했습니다.",
			qaQueryService.getQaReportDetail(authentication.getName(), projectId, qaReportId)
		);
	}
}
