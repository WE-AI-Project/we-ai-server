package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import com.weai.server.domain.project.domain.ProjectTechStack;
import com.weai.server.domain.project.domain.ProjectTechStackCategory;
import com.weai.server.domain.project.repository.ProjectMemberCountProjection;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.project.repository.ProjectTechStackRepository;
import com.weai.server.domain.project.request.ProjectCreateRequest;
import com.weai.server.domain.project.request.ProjectJoinRequest;
import com.weai.server.domain.project.request.ProjectTechStackRequest;
import com.weai.server.domain.project.response.MyProjectResponse;
import com.weai.server.domain.project.response.ProjectCreateResponse;
import com.weai.server.domain.project.response.ProjectJoinResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectTechStackRepository projectTechStackRepository;
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
				trimToNull(request.localPath()),
				today,
				request.deadlineDate(),
				creator
			));

			ProjectMember leader = projectMemberRepository.save(ProjectMember.leader(project, creator, leaderDepartment));
			saveTechStacks(project, request.techStacksOrEmpty());

			return ProjectCreateResponse.from(project, leader, techStackNames, today);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.PROJECT_CREATE_FAILED, "프로젝트를 저장하지 못했습니다.");
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
			throw new ApiException(ErrorCode.PROJECT_JOIN_FAILED, "프로젝트 참여를 완료하지 못했습니다.");
		}
	}

	private void validateCreateRequest(ProjectCreateRequest request, LocalDate today) {
		String projectName = trimToNull(request.projectName());
		if (projectName == null) {
			throw new ApiException(ErrorCode.PROJECT_NAME_REQUIRED);
		}
		if (projectName.length() < 2) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "프로젝트명은 2자 이상이어야 합니다.");
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

	private void validateProjectDeadline(LocalDate deadlineDate, LocalDate today) {
		if (deadlineDate != null && deadlineDate.isBefore(today)) {
			throw new ApiException(ErrorCode.INVALID_PROJECT_DATE);
		}
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
