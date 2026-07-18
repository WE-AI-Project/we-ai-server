package com.weai.server.domain.project.domain;

import com.weai.server.domain.user.domain.User;
import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "daily_standup_dismissals",
	indexes = {
		@Index(name = "idx_daily_standup_dismissals_project_user_date", columnList = "project_id, user_id, dismiss_date", unique = true)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DailyStandupDismissal extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "dismissal_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "dismiss_date", nullable = false)
	private LocalDate dismissDate;

	@Column(name = "dismissed_until", nullable = false)
	private LocalDateTime dismissedUntil;

	public static DailyStandupDismissal create(
		Project project,
		User user,
		LocalDate dismissDate,
		LocalDateTime dismissedUntil
	) {
		return DailyStandupDismissal.builder()
			.project(project)
			.user(user)
			.dismissDate(dismissDate)
			.dismissedUntil(dismissedUntil)
			.build();
	}
}
