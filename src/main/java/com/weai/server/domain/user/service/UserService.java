package com.weai.server.domain.user.service;

import com.weai.server.domain.auth.request.SignUpRequest;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectSchedule;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.project.repository.ProjectScheduleRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.domain.user.request.UserProfileUpdateRequest;
import com.weai.server.domain.user.response.UserActivitySummaryResponse;
import com.weai.server.domain.user.response.UserRecentActivityListResponse;
import com.weai.server.domain.user.response.UserResponse;
import com.weai.server.global.dto.PageResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectScheduleRepository projectScheduleRepository;

	private static final Set<ProjectScheduleStatus> COMPLETED_SCHEDULE_STATUSES = Set.of(
		ProjectScheduleStatus.DONE,
		ProjectScheduleStatus.COMPLETED
	);

	public PageResponse<UserResponse> findAll(Pageable pageable) {
		Pageable normalizedPageable = pageable.getSort().isSorted()
			? pageable
			: PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));

		return PageResponse.from(userRepository.findAll(normalizedPageable)
			.map(UserResponse::from)
		);
	}

	public UserResponse findById(Long userId) {
		return UserResponse.from(findUserEntityById(userId));
	}

	public User getUserEntityById(Long userId) {
		return findUserEntityById(userId);
	}

	public UserResponse findByEmail(String email) {
		return UserResponse.from(getUserEntityByEmail(email));
	}

	@Transactional
	public UserResponse updateMyProfile(String email, UserProfileUpdateRequest request) {
		User user = getUserEntityByEmail(email);
		String username = request.username() == null ? user.getUsername() : validateUsername(request.username());
		String name = request.name() == null ? user.getName() : validateName(request.name());

		if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
			throw new ApiException(ErrorCode.CONFLICT, "Username '%s' is already in use.".formatted(username));
		}

		user.updateProfile(username, name);
		return UserResponse.from(userRepository.saveAndFlush(user));
	}

	public UserActivitySummaryResponse getMyActivitySummary(String email) {
		User user = getUserEntityByEmail(email);
		long activeProjectCount = projectMemberRepository.countByUser_IdAndStatusAndProject_Status(
			user.getId(),
			ProjectMemberStatus.ACTIVE,
			ProjectStatus.ACTIVE
		);
		long leaderProjectCount = projectMemberRepository.countByUser_IdAndRoleAndStatusAndProject_Status(
			user.getId(),
			ProjectMemberRole.LEADER,
			ProjectMemberStatus.ACTIVE,
			ProjectStatus.ACTIVE
		);
		long assignedScheduleCount = projectScheduleRepository.countByAssignee_IdAndProject_Status(
			user.getId(),
			ProjectStatus.ACTIVE
		);
		long todoScheduleCount = projectScheduleRepository.countByAssignee_IdAndProject_StatusAndStatus(
			user.getId(),
			ProjectStatus.ACTIVE,
			ProjectScheduleStatus.TODO
		);
		long inProgressScheduleCount = projectScheduleRepository.countByAssignee_IdAndProject_StatusAndStatus(
			user.getId(),
			ProjectStatus.ACTIVE,
			ProjectScheduleStatus.IN_PROGRESS
		);
		long completedScheduleCount = projectScheduleRepository.countByAssignee_IdAndProject_StatusAndStatusIn(
			user.getId(),
			ProjectStatus.ACTIVE,
			COMPLETED_SCHEDULE_STATUSES
		);
		long holdScheduleCount = projectScheduleRepository.countByAssignee_IdAndProject_StatusAndStatus(
			user.getId(),
			ProjectStatus.ACTIVE,
			ProjectScheduleStatus.HOLD
		);

		return new UserActivitySummaryResponse(
			activeProjectCount,
			leaderProjectCount,
			assignedScheduleCount,
			todoScheduleCount,
			inProgressScheduleCount,
			completedScheduleCount,
			holdScheduleCount,
			calculateRate(completedScheduleCount, assignedScheduleCount)
		);
	}

	public UserRecentActivityListResponse getMyRecentActivities(String email, Integer limit) {
		User user = getUserEntityByEmail(email);
		int normalizedLimit = normalizeActivityLimit(limit);
		List<UserActivitySummary> activities = new ArrayList<>();

		projectRepository.findByCreatedBy_IdAndStatusOrderByCreatedAtDescIdDesc(user.getId(), ProjectStatus.ACTIVE)
			.forEach(project -> activities.add(toProjectCreatedActivity(project)));
		projectMemberRepository.findActiveProjectsByUserId(user.getId(), ProjectMemberStatus.ACTIVE, ProjectStatus.ACTIVE)
			.forEach(member -> activities.add(toProjectJoinedActivity(member)));
		projectScheduleRepository.findByAssigneeIdAndProjectStatusOrderByUpdatedAtDesc(user.getId(), ProjectStatus.ACTIVE)
			.forEach(schedule -> activities.add(toScheduleActivity(schedule)));

		List<UserRecentActivityListResponse.ActivityResponse> responses = activities.stream()
			.sorted(Comparator.comparing(UserActivitySummary::createdAt).reversed()
				.thenComparing(UserActivitySummary::activityId, Comparator.reverseOrder()))
			.limit(normalizedLimit)
			.map(activity -> new UserRecentActivityListResponse.ActivityResponse(
				activity.activityId(),
				activity.type(),
				activity.projectId(),
				activity.projectName(),
				activity.title(),
				activity.description(),
				activity.targetName(),
				activity.createdAt()
			))
			.toList();

		return new UserRecentActivityListResponse(normalizedLimit, responses);
	}

	public User getUserEntityByUsername(String username) {
		return userRepository.findByUsername(username)
			.orElseThrow(() -> new ApiException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"User with username '%s' could not be found.".formatted(username)
			));
	}

	public User getUserEntityByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> new ApiException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"User with email '%s' could not be found.".formatted(email)
			));
	}

	@Transactional
	public UserResponse registerUser(SignUpRequest request) {
		return registerUser(request, UserRole.USER);
	}

	@Transactional
	public void ensureBootstrapAdmin(String username, String rawPassword) {
		if (userRepository.existsByUsername(username)) {
			return;
		}

		User adminUser = User.create(
			username,
			passwordEncoder.encode(rawPassword),
			"Bootstrap Admin",
			username + "@local.we-ai",
			UserRole.ADMIN
		);

		userRepository.save(adminUser);
		log.info("Created bootstrap admin account '{}'.", username);
	}

	private UserResponse registerUser(SignUpRequest request, UserRole role) {
		String resolvedUsername = resolveUsername(request);
		validateDuplicateUser(resolvedUsername, request.email());

		User createdUser = userRepository.save(User.create(
			resolvedUsername,
			passwordEncoder.encode(request.password()),
			request.name(),
			request.email(),
			role
		));

		return UserResponse.from(createdUser);
	}

	private String resolveUsername(SignUpRequest request) {
		if (StringUtils.hasText(request.username())) {
			return request.username().trim();
		}

		String emailLocalPart = request.email().substring(0, request.email().indexOf('@'));
		String sanitizedBase = emailLocalPart
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9._-]", "");
		String baseUsername = StringUtils.hasText(sanitizedBase) ? sanitizedBase : "user";

		if (baseUsername.length() < 4) {
			baseUsername = (baseUsername + "user").substring(0, 4);
		}

		return generateUniqueUsername(baseUsername);
	}

	private String generateUniqueUsername(String baseUsername) {
		String normalizedBase = baseUsername.length() > 50 ? baseUsername.substring(0, 50) : baseUsername;
		String candidate = normalizedBase;
		int suffix = 1;

		while (userRepository.existsByUsername(candidate)) {
			String suffixValue = "-" + suffix++;
			int maxBaseLength = 50 - suffixValue.length();
			String truncatedBase = normalizedBase.substring(0, Math.min(normalizedBase.length(), maxBaseLength));
			candidate = truncatedBase + suffixValue;
		}

		return candidate;
	}

	private void validateDuplicateUser(String username, String email) {
		if (userRepository.existsByUsername(username)) {
			throw new ApiException(ErrorCode.CONFLICT, "Username '%s' is already in use.".formatted(username));
		}

		if (userRepository.existsByEmail(email)) {
			throw new ApiException(ErrorCode.CONFLICT, "Email '%s' is already in use.".formatted(email));
		}
	}

	private User findUserEntityById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ApiException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"User with id %d could not be found.".formatted(userId)
			));
	}

	private String validateUsername(String rawUsername) {
		String username = trimToNull(rawUsername);
		if (username == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "username must not be blank.");
		}
		if (username.length() > 50) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "username must be 50 characters or fewer.");
		}
		return username;
	}

	private String validateName(String rawName) {
		String name = trimToNull(rawName);
		if (name == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "name must not be blank.");
		}
		if (name.length() > 20) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "name must be 20 characters or fewer.");
		}
		return name;
	}

	private int normalizeActivityLimit(Integer limit) {
		if (limit == null) {
			return 10;
		}
		if (limit <= 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "limit must be greater than 0.");
		}
		return Math.min(limit, 100);
	}

	private int calculateRate(long completedCount, long totalCount) {
		if (totalCount == 0) {
			return 0;
		}
		return (int) ((completedCount * 100) / totalCount);
	}

	private UserActivitySummary toProjectCreatedActivity(Project project) {
		return new UserActivitySummary(
			"project-%d-created".formatted(project.getId()),
			"PROJECT_CREATED",
			project.getId(),
			project.getProjectName(),
			"Project created",
			"%s project was created.".formatted(project.getProjectName()),
			project.getProjectName(),
			project.getCreatedAt()
		);
	}

	private UserActivitySummary toProjectJoinedActivity(ProjectMember member) {
		Project project = member.getProject();
		return new UserActivitySummary(
			"member-%d-joined".formatted(member.getId()),
			"MEMBER_JOINED",
			project.getId(),
			project.getProjectName(),
			"Project joined",
			"You joined %s project.".formatted(project.getProjectName()),
			project.getProjectName(),
			member.getJoinedAt()
		);
	}

	private UserActivitySummary toScheduleActivity(ProjectSchedule schedule) {
		String type = "SCHEDULE_CREATED";
		String title = "Schedule created";
		LocalDateTime createdAt = schedule.getCreatedAt();

		if (COMPLETED_SCHEDULE_STATUSES.contains(schedule.getStatus()) && schedule.getUpdatedAt().isAfter(schedule.getCreatedAt())) {
			type = "SCHEDULE_DONE";
			title = "Schedule completed";
			createdAt = schedule.getUpdatedAt();
		} else if (schedule.getUpdatedAt().isAfter(schedule.getCreatedAt())) {
			type = "SCHEDULE_UPDATED";
			title = "Schedule updated";
			createdAt = schedule.getUpdatedAt();
		}

		return new UserActivitySummary(
			"schedule-%d-%s".formatted(schedule.getId(), type.toLowerCase(Locale.ROOT).replace('_', '-')),
			type,
			schedule.getProject().getId(),
			schedule.getProject().getProjectName(),
			title,
			"%s schedule in %s.".formatted(schedule.getTitle(), schedule.getProject().getProjectName()),
			schedule.getTitle(),
			createdAt
		);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record UserActivitySummary(
		String activityId,
		String type,
		Long projectId,
		String projectName,
		String title,
		String description,
		String targetName,
		LocalDateTime createdAt
	) {
	}
}
