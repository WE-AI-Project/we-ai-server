package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectSchedule;
import com.weai.server.domain.project.domain.ProjectSchedulePriority;
import com.weai.server.domain.project.domain.ProjectScheduleStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import com.weai.server.domain.project.domain.ProjectTechStack;
import com.weai.server.domain.project.domain.ProjectTechStackCategory;
import com.weai.server.domain.project.repository.ProjectMemberCountProjection;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.project.repository.ProjectScheduleRepository;
import com.weai.server.domain.project.repository.ProjectTechStackRepository;
import com.weai.server.domain.project.request.ProjectCreateRequest;
import com.weai.server.domain.project.request.ProjectJoinRequest;
import com.weai.server.domain.project.request.ProjectScheduleCreateRequest;
import com.weai.server.domain.project.request.ProjectScheduleStatusUpdateRequest;
import com.weai.server.domain.project.request.ProjectScheduleUpdateRequest;
import com.weai.server.domain.project.request.ProjectTechStackCreateRequest;
import com.weai.server.domain.project.request.ProjectTechStackRequest;
import com.weai.server.domain.project.request.ProjectTechStackUpdateRequest;
import com.weai.server.domain.project.response.MyProjectResponse;
import com.weai.server.domain.project.response.ProjectCreateResponse;
import com.weai.server.domain.project.response.ProjectDashboardResponse;
import com.weai.server.domain.project.response.ProjectDetailResponse;
import com.weai.server.domain.project.response.ProjectJoinResponse;
import com.weai.server.domain.project.response.ProjectMemberListResponse;
import com.weai.server.domain.project.response.ProjectScheduleCreateResponse;
import com.weai.server.domain.project.response.ProjectScheduleDeleteResponse;
import com.weai.server.domain.project.response.ProjectScheduleDetailResponse;
import com.weai.server.domain.project.response.ProjectScheduleListResponse;
import com.weai.server.domain.project.response.ProjectTechStackDeleteResponse;
import com.weai.server.domain.project.response.ProjectTechStackListResponse;
import com.weai.server.domain.project.response.ProjectTechStackResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

	private static final String PROJECT_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int PROJECT_CODE_LENGTH = 8;
	private static final int PROJECT_CODE_MAX_ATTEMPTS = 20;
	private static final Pattern PROJECT_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{8}$");
	private static final Set<ProjectScheduleStatus> COMPLETED_SCHEDULE_STATUSES = Set.of(
		ProjectScheduleStatus.DONE,
		ProjectScheduleStatus.COMPLETED
	);

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectTechStackRepository projectTechStackRepository;
	private final ProjectScheduleRepository projectScheduleRepository;
	private final UserRepository userRepository;
	private final UserService userService;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public ProjectCreateResponse createProject(String userEmail, ProjectCreateRequest request) {
		LocalDate today = LocalDate.now();
		validateCreateRequest(request, today);

		User creator = userService.getUserEntityByEmail(userEmail);
		String projectCode = generateProjectCode();
		ProjectDepartment leaderDepartment = resolveLeaderDepartment(request);
		List<String> techStackNames = extractTechStackNames(request.techStacksOrEmpty());

		try {
			Project project = projectRepository.save(Project.create(
				request.projectName().trim(),
				trimToNull(request.description()),
				projectCode,
				trimToNull(request.repositoryUrl()),
				trimToNull(request.localPath()),
				today,
				request.deadlineDate(),
				creator
			));

			ProjectMember leader = projectMemberRepository.save(ProjectMember.leader(project, creator, leaderDepartment));
			saveTechStacks(project, request.techStacksOrEmpty());

			return ProjectCreateResponse.from(project, leader, techStackNames, today);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.PROJECT_CREATE_FAILED, "Failed to persist the project.");
		}
	}

	public List<MyProjectResponse> getMyProjects(String userEmail) {
		LocalDate today = LocalDate.now();
		User user = userService.getUserEntityByEmail(userEmail);
		List<ProjectMember> projectMembers = projectMemberRepository.findActiveProjectsByUserId(
			user.getId(),
			ProjectMemberStatus.ACTIVE,
			ProjectStatus.ACTIVE
		);

		if (projectMembers.isEmpty()) {
			return List.of();
		}

		List<Long> projectIds = projectMembers.stream()
			.map(projectMember -> projectMember.getProject().getId())
			.toList();

		Map<Long, Long> memberCounts = projectMemberRepository.countActiveMembersByProjectIds(projectIds).stream()
			.collect(Collectors.toMap(ProjectMemberCountProjection::getProjectId, ProjectMemberCountProjection::getMemberCount));

		Map<Long, List<String>> techStacksByProjectId = projectTechStackRepository.findByProject_IdInOrderByProject_IdAscIdAsc(projectIds)
			.stream()
			.collect(Collectors.groupingBy(
				projectTechStack -> projectTechStack.getProject().getId(),
				LinkedHashMap::new,
				Collectors.mapping(ProjectTechStack::getName, Collectors.toList())
			));

		return projectMembers.stream()
			.map(projectMember -> MyProjectResponse.from(
				projectMember,
				techStacksByProjectId.getOrDefault(projectMember.getProject().getId(), List.of()),
				memberCounts.getOrDefault(projectMember.getProject().getId(), 0L),
				today
			))
			.toList();
	}

	@Transactional
	public ProjectJoinResponse joinProject(String userEmail, ProjectJoinRequest request) {
		validateJoinRequest(request);

		User user = userService.getUserEntityByEmail(userEmail);
		String normalizedProjectCode = request.projectCode().trim();
		Project project = projectRepository.findByProjectCode(normalizedProjectCode)
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));

		if (project.getStatus() != ProjectStatus.ACTIVE) {
			throw new ApiException(ErrorCode.PROJECT_NOT_ACTIVE);
		}

		ProjectMember existingMember = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), user.getId())
			.orElse(null);
		if (existingMember != null) {
			if (existingMember.isActive()) {
				throw new ApiException(ErrorCode.ALREADY_JOINED_PROJECT);
			}

			if (existingMember.isKicked()) {
				throw new ApiException(ErrorCode.FORBIDDEN, "You cannot rejoin a project that removed your membership.");
			}

			existingMember.reactivate(request.department());
			return ProjectJoinResponse.from(existingMember);
		}

		try {
			ProjectMember joinedMember = projectMemberRepository.save(ProjectMember.member(project, user, request.department()));
			return ProjectJoinResponse.from(joinedMember);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.PROJECT_JOIN_FAILED, "Failed to join the project.");
		}
	}

	public ProjectDetailResponse getProjectDetail(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = getAccessibleProject(projectId, user.getId());
		return ProjectDetailResponse.from(project);
	}

	public ProjectMemberListResponse getProjectMembers(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		getAccessibleProject(projectId, user.getId());
		List<ProjectMember> members = projectMemberRepository.findByProjectIdAndStatusWithUser(projectId, ProjectMemberStatus.ACTIVE);
		return ProjectMemberListResponse.from(projectId, members);
	}

	public ProjectTechStackListResponse getProjectTechStacks(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		getAccessibleProject(projectId, user.getId());
		List<ProjectTechStack> techStacks = projectTechStackRepository.findByProject_IdOrderByCategoryAscIdAsc(projectId);
		return ProjectTechStackListResponse.from(projectId, techStacks);
	}

	@Transactional
	public ProjectTechStackResponse createProjectTechStack(
		String userEmail,
		Long projectId,
		ProjectTechStackCreateRequest request
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = validateProjectAccess(projectId, user.getId());

		String name = validateTechStackName(request.name());
		ProjectTechStackCategory category = parseRequiredTechStackCategory(request.category());
		validateTechStackDuplicate(projectId, name, category, null);

		ProjectTechStack techStack = ProjectTechStack.create(
			project,
			name,
			trimToNull(request.version()),
			category,
			request.requiredOrDefaultFalse()
		);
		return ProjectTechStackResponse.from(projectTechStackRepository.save(techStack));
	}

	@Transactional
	public ProjectTechStackResponse updateProjectTechStack(
		String userEmail,
		Long projectId,
		Long techStackId,
		ProjectTechStackUpdateRequest request
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		validateProjectAccess(projectId, user.getId());
		ProjectTechStack techStack = getProjectTechStack(projectId, techStackId);

		String name = resolveUpdatedTechStackName(request.name(), techStack.getName());
		String version = request.version() == null ? techStack.getVersion() : trimToNull(request.version());
		ProjectTechStackCategory category = resolveTechStackCategory(request.category(), techStack.getCategory());
		boolean isRequired = request.isRequired() == null ? techStack.isRequired() : request.isRequired();

		validateTechStackDuplicate(projectId, name, category, techStackId);

		techStack.update(name, version, category, isRequired);
		return ProjectTechStackResponse.from(projectTechStackRepository.saveAndFlush(techStack));
	}

	@Transactional
	public ProjectTechStackDeleteResponse deleteProjectTechStack(String userEmail, Long projectId, Long techStackId) {
		User user = userService.getUserEntityByEmail(userEmail);
		validateProjectAccess(projectId, user.getId());
		ProjectTechStack techStack = getProjectTechStack(projectId, techStackId);
		projectTechStackRepository.delete(techStack);
		return ProjectTechStackDeleteResponse.from(techStack.getId());
	}

	public ProjectScheduleListResponse getProjectSchedules(
		String userEmail,
		Long projectId,
		ProjectDepartment department,
		ProjectScheduleStatus status,
		LocalDate startDate,
		LocalDate endDate
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		validateProjectAccess(projectId, user.getId());
		List<ProjectSchedule> schedules = projectScheduleRepository.findByProjectIdWithFilters(
			projectId,
			department,
			status,
			startDate,
			endDate
		);
		return ProjectScheduleListResponse.from(projectId, schedules);
	}

	@Transactional
	public ProjectScheduleCreateResponse createProjectSchedule(
		String userEmail,
		Long projectId,
		ProjectScheduleCreateRequest request
	) {
		String normalizedTitle = validateScheduleRequest(request);
		User currentUser = userService.getUserEntityByEmail(userEmail);
		Project project = validateProjectAccess(projectId, currentUser.getId());
		User assignee = resolveAssignee(projectId, currentUser, request.assigneeId());

		try {
			ProjectSchedule savedSchedule = projectScheduleRepository.save(ProjectSchedule.create(
				project,
				assignee,
				normalizedTitle,
				trimToNull(request.description()),
				request.department(),
				request.startDate(),
				request.endDate(),
				request.priorityOrDefault(),
				request.statusOrDefault()
			));
			return ProjectScheduleCreateResponse.from(savedSchedule);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.SCHEDULE_CREATE_FAILED, "Failed to persist the project schedule.");
		}
	}

	public ProjectScheduleDetailResponse getProjectScheduleDetail(String userEmail, Long projectId, Long scheduleId) {
		User user = userService.getUserEntityByEmail(userEmail);
		validateProjectAccess(projectId, user.getId());
		ProjectSchedule schedule = getProjectSchedule(projectId, scheduleId);
		return ProjectScheduleDetailResponse.from(schedule);
	}

	@Transactional
	public ProjectScheduleDetailResponse updateProjectSchedule(
		String userEmail,
		Long projectId,
		Long scheduleId,
		ProjectScheduleUpdateRequest request
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		validateProjectAccess(projectId, user.getId());
		ProjectSchedule schedule = getProjectSchedule(projectId, scheduleId);

		String title = resolveUpdatedTitle(request.title(), schedule.getTitle());
		String description = request.description() == null ? schedule.getDescription() : trimToNull(request.description());
		User assignee = resolveAssignee(projectId, schedule.getAssignee(), request.assigneeId());
		ProjectDepartment department = resolveDepartment(request.department(), schedule.getDepartment());
		LocalDate startDate = request.startDate() == null ? schedule.getStartDate() : request.startDate();
		LocalDate endDate = request.endDate() == null ? schedule.getEndDate() : request.endDate();
		ProjectSchedulePriority priority = resolvePriority(request.priority(), schedule.getPriority());
		ProjectScheduleStatus status = resolveScheduleStatus(request.status(), schedule.getStatus());

		validateScheduleDateRange(startDate, endDate);

		schedule.update(assignee, title, description, department, startDate, endDate, priority, status);
		ProjectSchedule savedSchedule = projectScheduleRepository.saveAndFlush(schedule);
		return ProjectScheduleDetailResponse.from(savedSchedule);
	}

	@Transactional
	public ProjectScheduleDetailResponse updateProjectScheduleStatus(
		String userEmail,
		Long projectId,
		Long scheduleId,
		ProjectScheduleStatusUpdateRequest request
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		validateProjectAccess(projectId, user.getId());
		ProjectSchedule schedule = getProjectSchedule(projectId, scheduleId);
		ProjectScheduleStatus status = parseRequiredScheduleStatus(request.status());

		schedule.changeStatus(status);
		ProjectSchedule savedSchedule = projectScheduleRepository.saveAndFlush(schedule);
		return ProjectScheduleDetailResponse.from(savedSchedule);
	}

	@Transactional
	public ProjectScheduleDeleteResponse deleteProjectSchedule(String userEmail, Long projectId, Long scheduleId) {
		User user = userService.getUserEntityByEmail(userEmail);
		validateProjectAccess(projectId, user.getId());
		ProjectSchedule schedule = getProjectSchedule(projectId, scheduleId);
		projectScheduleRepository.delete(schedule);
		return ProjectScheduleDeleteResponse.from(schedule.getId());
	}

	public ProjectDashboardResponse getProjectDashboard(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		Project project = getAccessibleProject(projectId, user.getId());
		long memberCount = projectMemberRepository.countByProject_IdAndStatus(projectId, ProjectMemberStatus.ACTIVE);
		List<ProjectSchedule> schedules = projectScheduleRepository.findByProject_IdOrderByStartDateAscIdAsc(projectId);
		long scheduleCount = schedules.size();
		long completedScheduleCount = schedules.stream().filter(this::isCompleted).count();
		int progressRate = calculateProgressRate(completedScheduleCount, scheduleCount);

		Map<ProjectDepartment, List<ProjectSchedule>> schedulesByDepartment = schedules.stream()
			.collect(Collectors.groupingBy(ProjectSchedule::getDepartment));

		List<ProjectDashboardResponse.DepartmentProgressResponse> departmentProgress = schedulesByDepartment.entrySet().stream()
			.sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal()))
			.map(entry -> {
				long completedCount = entry.getValue().stream().filter(this::isCompleted).count();
				return ProjectDashboardResponse.DepartmentProgressResponse.of(
					entry.getKey(),
					entry.getValue().size(),
					completedCount
				);
			})
			.toList();

		List<ProjectDashboardResponse.RecentScheduleResponse> recentSchedules = schedules.stream()
			.sorted(Comparator.comparing(ProjectSchedule::getCreatedAt).reversed()
				.thenComparing(ProjectSchedule::getId, Comparator.reverseOrder()))
			.limit(5)
			.map(ProjectDashboardResponse.RecentScheduleResponse::from)
			.toList();

		return ProjectDashboardResponse.of(
			project,
			memberCount,
			scheduleCount,
			completedScheduleCount,
			progressRate,
			departmentProgress,
			recentSchedules
		);
	}

	private void validateCreateRequest(ProjectCreateRequest request, LocalDate today) {
		String projectName = trimToNull(request.projectName());
		if (projectName == null) {
			throw new ApiException(ErrorCode.PROJECT_NAME_REQUIRED);
		}
		if (projectName.length() < 2) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "projectName must be at least 2 characters.");
		}
		if (projectName.length() > 50) {
			throw new ApiException(ErrorCode.PROJECT_NAME_TOO_LONG);
		}

		String localPath = trimToNull(request.localPath());
		if (localPath == null) {
			throw new ApiException(ErrorCode.PROJECT_PATH_REQUIRED);
		}

		validateProjectDeadline(request.deadlineDate(), today);
	}

	private void validateJoinRequest(ProjectJoinRequest request) {
		String projectCode = trimToNull(request.projectCode());
		if (projectCode == null) {
			throw new ApiException(ErrorCode.PROJECT_CODE_REQUIRED);
		}
		if (!PROJECT_CODE_PATTERN.matcher(projectCode).matches()) {
			throw new ApiException(ErrorCode.INVALID_PROJECT_CODE_FORMAT);
		}
	}

	private String validateScheduleRequest(ProjectScheduleCreateRequest request) {
		String title = validateScheduleTitle(request.title());
		if (request.department() == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "department is required.");
		}
		if (request.startDate() == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "startDate is required.");
		}
		if (request.endDate() == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "endDate is required.");
		}
		validateScheduleDateRange(request.startDate(), request.endDate());
		return title;
	}

	private void validateProjectDeadline(LocalDate deadlineDate, LocalDate today) {
		if (deadlineDate != null && deadlineDate.isBefore(today)) {
			throw new ApiException(ErrorCode.INVALID_PROJECT_DATE);
		}
	}

	private Project getAccessibleProject(Long projectId, Long userId) {
		return validateProjectAccess(projectId, userId);
	}

	public Project validateProjectAccess(Long projectId, Long userId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));

		if (project.getStatus() != ProjectStatus.ACTIVE) {
			throw new ApiException(ErrorCode.PROJECT_NOT_ACTIVE);
		}

		if (!projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(projectId, userId, ProjectMemberStatus.ACTIVE)) {
			throw new ApiException(ErrorCode.PROJECT_ACCESS_DENIED);
		}

		return project;
	}

	private User resolveAssignee(Long projectId, User currentUser, Long assigneeId) {
		if (assigneeId == null || assigneeId.equals(currentUser.getId())) {
			return currentUser;
		}

		User assignee = userRepository.findById(assigneeId)
			.orElseThrow(() -> new ApiException(ErrorCode.ASSIGNEE_NOT_FOUND));

		if (!projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(projectId, assignee.getId(), ProjectMemberStatus.ACTIVE)) {
			throw new ApiException(ErrorCode.ASSIGNEE_NOT_PROJECT_MEMBER);
		}

		return assignee;
	}

	private ProjectSchedule getProjectSchedule(Long projectId, Long scheduleId) {
		return projectScheduleRepository.findByProjectIdAndScheduleId(projectId, scheduleId)
			.orElseThrow(() -> new ApiException(ErrorCode.SCHEDULE_NOT_FOUND));
	}

	private ProjectTechStack getProjectTechStack(Long projectId, Long techStackId) {
		return projectTechStackRepository.findByProject_IdAndId(projectId, techStackId)
			.orElseThrow(() -> new ApiException(ErrorCode.TECH_STACK_NOT_FOUND));
	}

	private String resolveUpdatedTitle(String rawTitle, String currentTitle) {
		if (rawTitle == null) {
			return currentTitle;
		}

		return validateScheduleTitle(rawTitle);
	}

	private String validateScheduleTitle(String rawTitle) {
		String title = trimToNull(rawTitle);
		if (title == null) {
			throw new ApiException(ErrorCode.SCHEDULE_TITLE_REQUIRED);
		}
		return title;
	}

	private String resolveUpdatedTechStackName(String rawName, String currentName) {
		if (rawName == null) {
			return currentName;
		}
		return validateTechStackName(rawName);
	}

	private String validateTechStackName(String rawName) {
		String name = trimToNull(rawName);
		if (name == null) {
			throw new ApiException(ErrorCode.TECH_STACK_NAME_REQUIRED);
		}
		return name;
	}

	private void validateScheduleDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
			throw new ApiException(ErrorCode.INVALID_SCHEDULE_DATE);
		}
	}

	private ProjectDepartment resolveDepartment(String rawDepartment, ProjectDepartment currentDepartment) {
		if (rawDepartment == null) {
			return currentDepartment;
		}

		String normalizedDepartment = trimToNull(rawDepartment);
		if (normalizedDepartment == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "department is invalid.");
		}

		try {
			return ProjectDepartment.valueOf(normalizedDepartment.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "department is invalid.");
		}
	}

	private ProjectSchedulePriority resolvePriority(String rawPriority, ProjectSchedulePriority currentPriority) {
		if (rawPriority == null) {
			return currentPriority;
		}

		String normalizedPriority = trimToNull(rawPriority);
		if (normalizedPriority == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "priority is invalid.");
		}

		try {
			return ProjectSchedulePriority.valueOf(normalizedPriority.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "priority is invalid.");
		}
	}

	private ProjectScheduleStatus resolveScheduleStatus(String rawStatus, ProjectScheduleStatus currentStatus) {
		if (rawStatus == null) {
			return currentStatus;
		}

		String normalizedStatus = trimToNull(rawStatus);
		if (normalizedStatus == null) {
			throw new ApiException(ErrorCode.INVALID_SCHEDULE_STATUS);
		}

		try {
			return ProjectScheduleStatus.valueOf(normalizedStatus.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_SCHEDULE_STATUS);
		}
	}

	private ProjectScheduleStatus parseRequiredScheduleStatus(String rawStatus) {
		String normalizedStatus = trimToNull(rawStatus);
		if (normalizedStatus == null) {
			throw new ApiException(ErrorCode.SCHEDULE_STATUS_REQUIRED);
		}

		try {
			return ProjectScheduleStatus.valueOf(normalizedStatus.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_SCHEDULE_STATUS);
		}
	}

	private ProjectTechStackCategory parseRequiredTechStackCategory(String rawCategory) {
		String normalizedCategory = trimToNull(rawCategory);
		if (normalizedCategory == null) {
			throw new ApiException(ErrorCode.TECH_STACK_CATEGORY_REQUIRED);
		}

		try {
			return ProjectTechStackCategory.valueOf(normalizedCategory.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "tech stack category is invalid.");
		}
	}

	private ProjectTechStackCategory resolveTechStackCategory(
		String rawCategory,
		ProjectTechStackCategory currentCategory
	) {
		if (rawCategory == null) {
			return currentCategory;
		}
		return parseRequiredTechStackCategory(rawCategory);
	}

	private void validateTechStackDuplicate(
		Long projectId,
		String name,
		ProjectTechStackCategory category,
		Long currentTechStackId
	) {
		boolean exists = currentTechStackId == null
			? projectTechStackRepository.existsByProject_IdAndNameIgnoreCaseAndCategory(projectId, name, category)
			: projectTechStackRepository.existsByProject_IdAndNameIgnoreCaseAndCategoryAndIdNot(
				projectId,
				name,
				category,
				currentTechStackId
			);

		if (exists) {
			throw new ApiException(ErrorCode.TECH_STACK_ALREADY_EXISTS);
		}
	}

	private int calculateProgressRate(long completedCount, long totalCount) {
		if (totalCount == 0) {
			return 0;
		}
		return (int) ((completedCount * 100) / totalCount);
	}

	private boolean isCompleted(ProjectSchedule schedule) {
		return COMPLETED_SCHEDULE_STATUSES.contains(schedule.getStatus());
	}

	private void saveTechStacks(Project project, Collection<ProjectTechStackRequest> techStacks) {
		if (techStacks.isEmpty()) {
			return;
		}

		List<ProjectTechStack> entities = techStacks.stream()
			.filter(Objects::nonNull)
			.map(techStack -> ProjectTechStack.create(
				project,
				techStack.name().trim(),
				trimToNull(techStack.version()),
				techStack.category(),
				techStack.requiredOrDefaultFalse()
			))
			.toList();

		projectTechStackRepository.saveAll(entities);
	}

	private List<String> extractTechStackNames(Collection<ProjectTechStackRequest> techStacks) {
		return techStacks.stream()
			.filter(Objects::nonNull)
			.map(ProjectTechStackRequest::name)
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(name -> !name.isEmpty())
			.toList();
	}

	private ProjectDepartment resolveLeaderDepartment(ProjectCreateRequest request) {
		if (request.department() != null) {
			return request.department();
		}

		return request.techStacksOrEmpty().stream()
			.filter(Objects::nonNull)
			.map(ProjectTechStackRequest::category)
			.filter(Objects::nonNull)
			.findFirst()
			.map(this::mapCategoryToDepartment)
			.orElse(ProjectDepartment.BACKEND);
	}

	private ProjectDepartment mapCategoryToDepartment(ProjectTechStackCategory category) {
		return switch (category) {
			case BACKEND, BUILD_TOOL -> ProjectDepartment.BACKEND;
			case FRONTEND -> ProjectDepartment.FRONTEND;
			case DEVOPS -> ProjectDepartment.DEVOPS;
			case AI -> ProjectDepartment.AI;
			case DATABASE -> ProjectDepartment.DATABASE;
			case LANGUAGE, ETC -> ProjectDepartment.BACKEND;
		};
	}

	private String generateProjectCode() {
		for (int attempt = 0; attempt < PROJECT_CODE_MAX_ATTEMPTS; attempt++) {
			String candidate = secureRandomCode();
			if (!projectRepository.existsByProjectCode(candidate)) {
				return candidate;
			}
		}

		throw new ApiException(ErrorCode.PROJECT_CODE_GENERATION_FAILED);
	}

	private String secureRandomCode() {
		StringBuilder builder = new StringBuilder(PROJECT_CODE_LENGTH);
		for (int i = 0; i < PROJECT_CODE_LENGTH; i++) {
			int index = secureRandom.nextInt(PROJECT_CODE_CHARACTERS.length());
			builder.append(PROJECT_CODE_CHARACTERS.charAt(index));
		}
		return builder.toString();
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
