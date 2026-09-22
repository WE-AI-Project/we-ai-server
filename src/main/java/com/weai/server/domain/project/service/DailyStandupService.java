package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.DailyStandupDismissal;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectSchedule;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import com.weai.server.domain.project.domain.ProjectTechStack;
import com.weai.server.domain.project.repository.DailyStandupDismissalRepository;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectScheduleRepository;
import com.weai.server.domain.project.repository.ProjectTechStackRepository;
import com.weai.server.domain.project.response.DailyStandupActivityResponse;
import com.weai.server.domain.project.response.DailyStandupDismissResponse;
import com.weai.server.domain.project.response.DailyStandupItemResponse;
import com.weai.server.domain.project.response.DailyStandupMemberResponse;
import com.weai.server.domain.project.response.DailyStandupSummaryResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyStandupService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
	private static final int RECENT_ACTIVITY_LIMIT = 20;

	private final ProjectService projectService;
	private final UserService userService;
	private final ProjectScheduleRepository projectScheduleRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectTechStackRepository projectTechStackRepository;
	private final DailyStandupDismissalRepository dailyStandupDismissalRepository;

	public DailyStandupSummaryResponse getSummary(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		ProjectMember currentMember = projectMemberRepository.findByProject_IdAndUser_IdAndStatus(
				projectId,
				user.getId(),
				ProjectMemberStatus.ACTIVE
			)
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_ACCESS_DENIED));
		LocalDate today = LocalDate.now(SEOUL_ZONE);
		LocalDateTime currentAccessedAt = LocalDateTime.now(SEOUL_ZONE);
		LocalDateTime lastAccessedAt = currentMember.getLastAccessedAt();
		LocalDateTime startAt = lastAccessedAt != null ? lastAccessedAt : today.atStartOfDay();
		LocalDateTime endAt = currentAccessedAt;

		try {
			boolean shouldShow = !dailyStandupDismissalRepository.existsByProject_IdAndUser_IdAndDismissDate(
				projectId,
				user.getId(),
				today
			);
			List<ProjectSchedule> schedules = projectScheduleRepository.findDailyStandupSchedules(projectId, startAt, endAt);
			List<DailyStandupItemResponse> completedItems = filterScheduleItems(schedules, ProjectScheduleStatus.DONE, ProjectScheduleStatus.COMPLETED);
			List<DailyStandupItemResponse> inProgressItems = filterScheduleItems(schedules, ProjectScheduleStatus.IN_PROGRESS);
			List<DailyStandupItemResponse> blockerItems = filterScheduleItems(schedules, ProjectScheduleStatus.HOLD);
			long todoCount = schedules.stream()
				.filter(schedule -> schedule.getStatus() == ProjectScheduleStatus.TODO)
				.count();
			List<DailyStandupActivityResponse> recentActivities = buildRecentActivities(project, projectId, startAt, endAt);
			DailyStandupSummaryResponse.Summary summary = new DailyStandupSummaryResponse.Summary(
				schedules.size() + recentActivities.size(),
				completedItems.size(),
				inProgressItems.size(),
				todoCount,
				blockerItems.size()
			);
			List<DailyStandupMemberResponse> members = buildMemberBriefings(projectId, schedules, currentMember);

			return new DailyStandupSummaryResponse(
				project.getId(),
				project.getProjectName(),
				today,
				shouldShow,
				lastAccessedAt,
				currentAccessedAt,
				startAt,
				endAt,
				summary,
				completedItems,
				inProgressItems,
				blockerItems,
				recentActivities,
				members
			);
		} catch (ApiException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new ApiException(ErrorCode.DAILY_STANDUP_SUMMARY_FAILED, "Failed to build daily standup summary.");
		}
	}

	@Transactional
	public DailyStandupDismissResponse dismissToday(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = projectService.validateProjectAccess(projectId, user.getId());
		LocalDate today = LocalDate.now(SEOUL_ZONE);
		LocalDateTime dismissedUntil = today.plusDays(1).atStartOfDay();

		try {
			dailyStandupDismissalRepository.findByProject_IdAndUser_IdAndDismissDate(projectId, user.getId(), today)
				.orElseGet(() -> dailyStandupDismissalRepository.save(DailyStandupDismissal.create(
					project,
					user,
					today,
					dismissedUntil
				)));
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.DAILY_STANDUP_DISMISS_FAILED, "Failed to save daily standup dismissal.");
		}

		return new DailyStandupDismissResponse(project.getId(), user.getId(), today, dismissedUntil, false);
	}

	// 부서/블로커 기준의 단순하지만 설명 가능한 연관도 판정 - 팀원 각각의 항목을 실제로 채워주고,
	// "나에게 관련"이 왜 그런지 사용자에게 정직하게 보여줄 수 있는 최소한의 규칙이다.
	private List<DailyStandupMemberResponse> buildMemberBriefings(
		Long projectId,
		List<ProjectSchedule> schedules,
		ProjectMember viewer
	) {
		Map<Long, List<ProjectSchedule>> schedulesByAssigneeId = schedules.stream()
			.collect(Collectors.groupingBy(schedule -> schedule.getAssignee().getId()));

		return projectMemberRepository.findByProjectIdAndStatusWithUser(projectId, ProjectMemberStatus.ACTIVE)
			.stream()
			.map(member -> toMemberBriefing(
				member,
				schedulesByAssigneeId.getOrDefault(member.getUser().getId(), Collections.emptyList()),
				viewer
			))
			.toList();
	}

	private DailyStandupMemberResponse toMemberBriefing(
		ProjectMember member,
		List<ProjectSchedule> memberSchedules,
		ProjectMember viewer
	) {
		List<DailyStandupItemResponse> completed = filterScheduleItems(memberSchedules, ProjectScheduleStatus.DONE, ProjectScheduleStatus.COMPLETED);
		List<DailyStandupItemResponse> inProgress = filterScheduleItems(memberSchedules, ProjectScheduleStatus.IN_PROGRESS);
		List<DailyStandupItemResponse> blockers = filterScheduleItems(memberSchedules, ProjectScheduleStatus.HOLD);

		boolean isViewer = member.getUser().getId().equals(viewer.getUser().getId());
		boolean hasBlockers = !blockers.isEmpty();
		boolean sameDepartment = member.getDepartment() == viewer.getDepartment();
		boolean relevantToMe = !isViewer && (hasBlockers || sameDepartment);

		String relevantReason = "";
		String relevantAction = "일정 확인";
		if (relevantToMe) {
			if (hasBlockers) {
				relevantReason = "블로커가 있어 팀 진행에 영향을 줄 수 있어요.";
				relevantAction = "블로커 확인";
			} else {
				relevantReason = "같은 %s 소속이라 관련 작업이 있어요.".formatted(member.getDepartment().name());
			}
		}

		return new DailyStandupMemberResponse(
			member.getUser().getName(),
			member.getDepartment(),
			completed,
			inProgress,
			blockers,
			relevantToMe,
			relevantReason,
			relevantAction,
			"Calendar",
			member.getLastAccessedAt()
		);
	}

	private List<DailyStandupItemResponse> filterScheduleItems(
		List<ProjectSchedule> schedules,
		ProjectScheduleStatus... statuses
	) {
		return schedules.stream()
			.filter(schedule -> hasStatus(schedule, statuses))
			.map(this::toItemResponse)
			.toList();
	}

	private boolean hasStatus(ProjectSchedule schedule, ProjectScheduleStatus... statuses) {
		for (ProjectScheduleStatus status : statuses) {
			if (schedule.getStatus() == status) {
				return true;
			}
		}
		return false;
	}

	private DailyStandupItemResponse toItemResponse(ProjectSchedule schedule) {
		return new DailyStandupItemResponse(
			"SCHEDULE",
			schedule.getTitle(),
			schedule.getDescription(),
			schedule.getAssignee().getName(),
			schedule.getDepartment(),
			schedule.getUpdatedAt()
		);
	}

	private List<DailyStandupActivityResponse> buildRecentActivities(
		Project project,
		Long projectId,
		LocalDateTime startAt,
		LocalDateTime endAt
	) {
		List<DailyStandupActivityResponse> memberActivities = projectMemberRepository.findDailyStandupJoinedMembers(
				projectId,
				startAt,
				endAt
			)
			.stream()
			.map(this::toMemberActivity)
			.toList();
		List<DailyStandupActivityResponse> techStackActivities = projectTechStackRepository.findDailyStandupTechStacks(
				projectId,
				startAt,
				endAt
			)
			.stream()
			.map(techStack -> toTechStackActivity(project.getCreatedBy().getName(), techStack))
			.toList();

		return java.util.stream.Stream.concat(memberActivities.stream(), techStackActivities.stream())
			.sorted(Comparator.comparing(DailyStandupActivityResponse::createdAt).reversed())
			.limit(RECENT_ACTIVITY_LIMIT)
			.toList();
	}

	private DailyStandupActivityResponse toMemberActivity(ProjectMember member) {
		return new DailyStandupActivityResponse(
			"MEMBER_JOINED",
			"프로젝트 멤버 참여",
			"%s 님이 프로젝트에 참여했습니다.".formatted(member.getUser().getName()),
			member.getUser().getName(),
			member.getJoinedAt()
		);
	}

	private DailyStandupActivityResponse toTechStackActivity(String actorName, ProjectTechStack techStack) {
		if (techStack.getUpdatedAt().isAfter(techStack.getCreatedAt())) {
			return new DailyStandupActivityResponse(
				"TECH_STACK_UPDATED",
				"기술 스택 수정",
				"%s 기술 스택 정보가 수정되었습니다.".formatted(techStack.getName()),
				actorName,
				techStack.getUpdatedAt()
			);
		}

		return new DailyStandupActivityResponse(
			"TECH_STACK_ADDED",
			"기술 스택 추가",
			"%s 기술 스택이 추가되었습니다.".formatted(techStack.getName()),
			actorName,
			techStack.getCreatedAt()
		);
	}
}
