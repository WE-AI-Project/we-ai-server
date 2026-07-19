package com.weai.server.domain.notification.response;

import com.weai.server.domain.notification.domain.Notification;
import com.weai.server.domain.notification.domain.NotificationTargetType;
import com.weai.server.domain.notification.domain.NotificationType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "알림 목록 조회 응답")
public record NotificationListResponse(
	@Schema(description = "프로젝트 ID", example = "1")
	Long projectId,

	@Schema(description = "읽지 않은 알림 수", example = "3")
	long unreadCount,

	@Schema(description = "필터 조건에 맞는 전체 알림 수", example = "10")
	long totalCount,

	@Schema(description = "현재 페이지 번호", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 페이지 수", example = "1")
	int totalPages,

	@ArraySchema(schema = @Schema(implementation = NotificationResponse.class))
	List<NotificationResponse> notifications
) {

	public static NotificationListResponse from(Long projectId, long unreadCount, Page<Notification> notificationPage) {
		return new NotificationListResponse(
			projectId,
			unreadCount,
			notificationPage.getTotalElements(),
			notificationPage.getNumber(),
			notificationPage.getSize(),
			notificationPage.getTotalPages(),
			notificationPage.getContent().stream().map(NotificationResponse::from).toList()
		);
	}

	@Schema(description = "알림 목록 아이템")
	public record NotificationResponse(
		@Schema(description = "알림 ID", example = "100")
		Long notificationId,

		@Schema(description = "알림 유형", example = "SCHEDULE")
		NotificationType type,

		@Schema(description = "알림 제목", example = "일정 상태 변경")
		String title,

		@Schema(description = "알림 내용", example = "프로젝트 일정 상세 조회 API 작업이 완료되었습니다.")
		String message,

		@Schema(description = "관련 대상 유형", example = "SCHEDULE")
		NotificationTargetType targetType,

		@Schema(description = "관련 대상 ID", example = "10")
		Long targetId,

		@Schema(description = "프론트 이동 경로", example = "/projects/1/schedules/10")
		String linkUrl,

		@Schema(description = "읽음 여부", example = "false")
		boolean isRead,

		@Schema(description = "읽은 시각", example = "2026-05-25T11:00:00")
		LocalDateTime readAt,

		@Schema(description = "생성 시각", example = "2026-05-25T10:30:00")
		LocalDateTime createdAt
	) {

		public static NotificationResponse from(Notification notification) {
			return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				notification.getTitle(),
				notification.getMessage(),
				notification.getTargetType(),
				notification.getTargetId(),
				notification.getLinkUrl(),
				notification.isRead(),
				notification.getReadAt(),
				notification.getCreatedAt()
			);
		}
	}
}
