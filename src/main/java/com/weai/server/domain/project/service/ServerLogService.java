package com.weai.server.domain.project.service;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ServerLog;
import com.weai.server.domain.project.domain.ServerLogLevel;
import com.weai.server.domain.project.domain.ServerLogSource;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ServerLogRepository;
import com.weai.server.domain.project.response.ServerLogClearResponse;
import com.weai.server.domain.project.response.ServerLogListResponse;
import com.weai.server.domain.project.response.ServerLogListResponse.ServerLogFiltersResponse;
import com.weai.server.domain.project.response.ServerLogResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerLogService {

	private static final int DEFAULT_PAGE_SIZE = 100;
	private static final int MAX_PAGE_SIZE = 500;

	private final ProjectService projectService;
	private final UserService userService;
	private final ProjectMemberRepository projectMemberRepository;
	private final ServerLogRepository serverLogRepository;
	private final ServerLogStreamService serverLogStreamService;

	public ServerLogListResponse getLogs(String userEmail, Long projectId, Integer rawPage, Integer rawSize) {
		getAccessibleProject(userEmail, projectId);
		Page<ServerLog> logs = serverLogRepository.findByProject_IdAndDeletedAtIsNull(projectId, pageRequest(rawPage, rawSize));
		return toListResponse(projectId, logs, null);
	}

	public ServerLogListResponse searchLogs(
		String userEmail,
		Long projectId,
		String rawKeyword,
		String rawLevel,
		String rawSource,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime,
		Integer rawPage,
		Integer rawSize
	) {
		getAccessibleProject(userEmail, projectId);
		LogFilters filters = parseFilters(rawKeyword, rawLevel, rawSource, startDateTime, endDateTime);
		Page<ServerLog> logs = serverLogRepository.findAll(searchSpecification(projectId, filters), pageRequest(rawPage, rawSize));
		return toListResponse(projectId, logs, filters.toResponse());
	}

	public SseEmitter streamLogs(String userEmail, Long projectId, String rawLevel, String rawSource, String rawKeyword) {
		getAccessibleProject(userEmail, projectId);
		LogFilters filters = parseFilters(rawKeyword, rawLevel, rawSource, null, null);
		try {
			return serverLogStreamService.subscribe(projectId, filters.level(), filters.source(), filters.keyword());
		} catch (RuntimeException exception) {
			throw new ApiException(ErrorCode.SERVER_LOG_STREAM_FAILED, "Failed to create server log stream.");
		}
	}

	@Transactional
	public ServerLogClearResponse clearLogs(String userEmail, Long projectId) {
		Project project = getAccessibleProject(userEmail, projectId);
		User user = userService.getUserEntityByEmail(userEmail);
		ProjectMember member = projectMemberRepository
			.findByProject_IdAndUser_IdAndStatus(projectId, user.getId(), ProjectMemberStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_ACCESS_DENIED));
		if (!member.isLeader()) {
			throw new ApiException(ErrorCode.PROJECT_LEADER_ONLY);
		}

		LocalDateTime clearedAt = LocalDateTime.now();
		try {
			int clearedCount = serverLogRepository.softDeleteAllByProjectId(project.getId(), clearedAt);
			return new ServerLogClearResponse(project.getId(), clearedCount, clearedAt);
		} catch (RuntimeException exception) {
			throw new ApiException(ErrorCode.SERVER_LOG_CLEAR_FAILED, "Failed to clear server logs.");
		}
	}

	@Transactional
	public ServerLogResponse recordLog(
		Project project,
		ServerLogLevel level,
		ServerLogSource source,
		String message,
		String threadName,
		String loggerName,
		String traceId
	) {
		ServerLog savedLog = serverLogRepository.save(ServerLog.create(
			project, level, source, message, threadName, loggerName, traceId
		));
		serverLogStreamService.publish(savedLog);
		return ServerLogResponse.from(savedLog);
	}

	private ServerLogListResponse toListResponse(
		Long projectId,
		Page<ServerLog> logs,
		ServerLogFiltersResponse filters
	) {
		return new ServerLogListResponse(
			projectId,
			serverLogRepository.countByProject_IdAndLevelAndDeletedAtIsNull(projectId, ServerLogLevel.ERROR),
			serverLogRepository.countByProject_IdAndLevelAndDeletedAtIsNull(projectId, ServerLogLevel.WARN),
			serverLogRepository.countByProject_IdAndLevelAndDeletedAtIsNull(projectId, ServerLogLevel.INFO),
			logs.getTotalElements(),
			logs.getNumber(),
			logs.getSize(),
			logs.getTotalPages(),
			filters,
			logs.getContent().stream().map(ServerLogResponse::from).toList()
		);
	}

	private Specification<ServerLog> searchSpecification(Long projectId, LogFilters filters) {
		return (root, query, criteriaBuilder) -> {
			List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("project").get("id"), projectId));
			predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
			if (filters.keyword() != null) {
				predicates.add(criteriaBuilder.like(
					criteriaBuilder.lower(root.get("message")), "%" + filters.keyword().toLowerCase(Locale.ROOT) + "%"
				));
			}
			if (filters.level() != null) {
				predicates.add(criteriaBuilder.equal(root.get("level"), filters.level()));
			}
			if (filters.source() != null) {
				predicates.add(criteriaBuilder.equal(root.get("source"), filters.source()));
			}
			if (filters.startDateTime() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filters.startDateTime()));
			}
			if (filters.endDateTime() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filters.endDateTime()));
			}
			return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	private Pageable pageRequest(Integer rawPage, Integer rawSize) {
		int page = rawPage == null ? 0 : rawPage;
		int size = rawSize == null ? DEFAULT_PAGE_SIZE : rawSize;
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "page must be non-negative and size must be between 1 and 500.");
		}
		return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	private LogFilters parseFilters(
		String rawKeyword,
		String rawLevel,
		String rawSource,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime
	) {
		String keyword = trimToNull(rawKeyword);
		if (keyword != null && keyword.length() > 100) {
			throw new ApiException(ErrorCode.LOG_KEYWORD_TOO_LONG);
		}
		if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
			throw new ApiException(ErrorCode.INVALID_LOG_DATE_RANGE);
		}
		return new LogFilters(keyword, parseLevel(rawLevel), parseSource(rawSource), startDateTime, endDateTime);
	}

	private ServerLogLevel parseLevel(String rawLevel) {
		String level = trimToNull(rawLevel);
		if (level == null) {
			return null;
		}
		try {
			return ServerLogLevel.valueOf(level.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_SERVER_LOG_LEVEL);
		}
	}

	private ServerLogSource parseSource(String rawSource) {
		String source = trimToNull(rawSource);
		if (source == null) {
			return null;
		}
		try {
			return ServerLogSource.valueOf(source.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_SERVER_LOG_SOURCE);
		}
	}

	private Project getAccessibleProject(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		return projectService.validateProjectAccess(projectId, user.getId());
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private record LogFilters(
		String keyword,
		ServerLogLevel level,
		ServerLogSource source,
		LocalDateTime startDateTime,
		LocalDateTime endDateTime
	) {
		private ServerLogFiltersResponse toResponse() {
			return new ServerLogFiltersResponse(
				keyword,
				level == null ? null : level.name(),
				source == null ? null : source.name(),
				startDateTime,
				endDateTime
			);
		}
	}
}
