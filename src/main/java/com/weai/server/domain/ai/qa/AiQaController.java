package com.weai.server.domain.ai.qa;

import com.weai.server.domain.ai.support.AiRateLimiterService;
import com.weai.server.domain.notification.domain.NotificationTargetType;
import com.weai.server.domain.notification.domain.NotificationType;
import com.weai.server.domain.notification.event.NotificationRequestedEvent;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "AI QA", description = "Diff-based code QA and semantic commit generation API.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiQaController {

	private static final int MAX_CALLS_PER_MINUTE = 15;

	private final AiQaService aiQaService;
	private final QaPersistenceService qaPersistenceService;
	private final UserService userService;
	private final ProjectService projectService;
	private final ApplicationEventPublisher eventPublisher;
	private final AiRateLimiterService aiRateLimiterService;

	@Operation(
		summary = "Analyze code diff",
		description = "Retrieves project-isolated RAG context, then analyzes a git diff and returns bug risk, optimization advice, and a semantic commit message."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/qa")
	public ApiResponse<QaResponse> analyze(
		Authentication authentication,
		@Valid @RequestBody QaRequest request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());
		aiRateLimiterService.checkAndConsume("qa:" + user.getId(), MAX_CALLS_PER_MINUTE, Duration.ofMinutes(1).toMillis());

		QaResponse response;
		try {
			response = aiQaService.analyze(request.projectId(), request.diff());
		} catch (RuntimeException exception) {
			persistQaFailure(request, exception);
			throw exception;
		}
		persistQaReport(request, response);
		notifyQaCompleted(request.projectId(), user.getId(), response);

		return ApiResponse.success(
			"AI_QA_SUCCESS",
			"AI QA analysis completed successfully.",
			response
		);
	}

	// QA 히스토리 저장은 부가 기능이라, 실패해도 방금 완료된 분석 결과 응답 자체는 그대로 내려준다.
	private void persistQaReport(QaRequest request, QaResponse response) {
		try {
			qaPersistenceService.persist(request.projectId(), request.commitId(), response);
		} catch (RuntimeException exception) {
			log.warn("Failed to persist QA report for projectId={}", request.projectId(), exception);
		}
	}

	// AI 호출 자체가 실패한 경우에도 QA 실행 이력에 FAILED 레코드를 남긴다. 이전에는 analyze()가
	// 던진 예외가 persistQaReport 호출부까지 아예 도달하지 못해, 실패한 QA는 DB에 어떤 흔적도
	// 남기지 않고 "진행 상태 조회" API에서도 절대 보이지 않았다.
	private void persistQaFailure(QaRequest request, RuntimeException exception) {
		try {
			qaPersistenceService.persistFailure(request.projectId(), request.commitId(), exception.getMessage());
		} catch (RuntimeException persistException) {
			log.warn("Failed to persist QA failure record for projectId={}", request.projectId(), persistException);
		}
	}

	private void notifyQaCompleted(Long projectId, Long userId, QaResponse response) {
		eventPublisher.publishEvent(new NotificationRequestedEvent(
			projectId,
			List.of(userId),
			NotificationType.QA,
			"AI QA 분석 완료",
			response.commitMsg(),
			NotificationTargetType.QA,
			null,
			null
		));
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
