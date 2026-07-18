package com.weai.server.domain.project.controller;

import com.weai.server.domain.project.request.DailyStandupDismissRequest;
import com.weai.server.domain.project.response.DailyStandupDismissResponse;
import com.weai.server.domain.project.response.DailyStandupSummaryResponse;
import com.weai.server.domain.project.service.DailyStandupService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Daily Standup", description = "알림 > 데일리 스탠드업 모달 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class DailyStandupController {

	private final DailyStandupService dailyStandupService;

	@Operation(
		summary = "데일리 스탠드업 요약 조회",
		description = "프로젝트 입장 시 표시되는 데일리 스탠드업 모달의 요약 데이터를 조회합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.DAILY_STANDUP_SUMMARY_FAILED
	})
	@GetMapping("/{projectId}/daily-standup")
	public ApiResponse<DailyStandupSummaryResponse> getDailyStandupSummary(
		Authentication authentication,
		@PathVariable Long projectId
	) {
		return ApiResponse.success(
			"DAILY_STANDUP_SUMMARY_SUCCESS",
			"데일리 스탠드업 요약 조회에 성공했습니다.",
			dailyStandupService.getSummary(authentication.getName(), projectId)
		);
	}

	@Operation(
		summary = "오늘 다시 보지 않기 저장",
		description = "오늘 다시 보지 않기 버튼 클릭 시 해당 사용자와 프로젝트 기준으로 오늘 하루 모달을 숨김 처리합니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.UNAUTHORIZED,
		ErrorCode.PROJECT_NOT_FOUND,
		ErrorCode.PROJECT_NOT_ACTIVE,
		ErrorCode.PROJECT_ACCESS_DENIED,
		ErrorCode.DAILY_STANDUP_DISMISS_FAILED
	})
	@PostMapping("/{projectId}/daily-standup/dismiss")
	public ApiResponse<DailyStandupDismissResponse> dismissDailyStandup(
		Authentication authentication,
		@PathVariable Long projectId,
		@RequestBody(required = false) DailyStandupDismissRequest request
	) {
		return ApiResponse.success(
			"DAILY_STANDUP_DISMISS_SUCCESS",
			"오늘 다시 보지 않기가 저장되었습니다.",
			dailyStandupService.dismissToday(authentication.getName(), projectId)
		);
	}
}
