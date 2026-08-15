package com.weai.server.domain.ai.qa;

import com.weai.server.domain.ai.qa.domain.QaReport;
import com.weai.server.domain.ai.qa.domain.QaReportStatus;
import com.weai.server.domain.ai.qa.domain.QaRun;
import com.weai.server.domain.ai.qa.repository.QaReportRepository;
import com.weai.server.domain.ai.qa.repository.QaReportTestResultRepository;
import com.weai.server.domain.ai.qa.repository.QaRunRepository;
import com.weai.server.domain.ai.qa.response.CommitQaResultResponse;
import com.weai.server.domain.ai.qa.response.QaReportDetailResponse;
import com.weai.server.domain.ai.qa.response.QaReportListResponse;
import com.weai.server.domain.ai.qa.response.QaRunStatusResponse;
import com.weai.server.domain.project.response.ProjectCommitDetailResponse;
import com.weai.server.domain.project.service.ProjectGitService;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QaQueryService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final ProjectService projectService;
	private final UserService userService;
	private final ProjectGitService projectGitService;
	private final QaRunRepository qaRunRepository;
	private final QaReportRepository qaReportRepository;
	private final QaReportTestResultRepository qaReportTestResultRepository;

	public QaRunStatusResponse getQaRunStatus(String userEmail, Long projectId, Long qaRunId) {
		validateProjectAccess(userEmail, projectId);
		QaRun qaRun = qaRunRepository.findByIdAndProject_Id(qaRunId, projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.QA_RUN_NOT_FOUND));

		return QaRunStatusResponse.from(qaRun);
	}

	public QaReportListResponse getQaReports(
		String userEmail,
		Long projectId,
		Integer page,
		Integer size,
		String rawStatus,
		String commitId
	) {
		validateProjectAccess(userEmail, projectId);
		PageRequest pageRequest = PageRequest.of(
			normalizePage(page),
			normalizeSize(size),
			Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
		);
		Page<QaReport> reports = qaReportRepository.findPageByProjectIdAndFilters(
			projectId,
			parseReportStatus(rawStatus),
			trimToNull(commitId),
			pageRequest
		);

		return QaReportListResponse.from(projectId, reports);
	}

	public QaReportDetailResponse getQaReportDetail(String userEmail, Long projectId, Long qaReportId) {
		validateProjectAccess(userEmail, projectId);
		QaReport report = qaReportRepository.findDetailWithIssuesByIdAndProjectId(qaReportId, projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.QA_REPORT_NOT_FOUND));

		return QaReportDetailResponse.from(
			report,
			qaReportTestResultRepository.findByQaReport_IdOrderByIdAsc(report.getId())
		);
	}

	public CommitQaResultResponse getCommitQaResult(
		String userEmail,
		Long projectId,
		String commitId,
		String repositoryType
	) {
		validateProjectAccess(userEmail, projectId);
		String normalizedCommitId = trimToNull(commitId);
		if (normalizedCommitId == null) {
			throw new ApiException(ErrorCode.COMMIT_NOT_FOUND);
		}

		ProjectCommitDetailResponse commit = getCommitDetail(userEmail, projectId, repositoryType, normalizedCommitId);
		List<QaReport> reports = qaReportRepository.findByProjectIdAndCommitIdOrderByLatest(
			projectId,
			normalizedCommitId
		);

		if (reports.isEmpty() && !normalizedCommitId.equals(commit.commitHash())) {
			reports = qaReportRepository.findByProjectIdAndCommitIdOrderByLatest(projectId, commit.commitHash());
		}

		return CommitQaResultResponse.from(commit, reports);
	}

	private void validateProjectAccess(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
	}

	private ProjectCommitDetailResponse getCommitDetail(
		String userEmail,
		Long projectId,
		String repositoryType,
		String commitId
	) {
		try {
			return projectGitService.getProjectCommitDetail(
				userEmail,
				projectId,
				trimToNull(repositoryType) == null ? "BACKEND" : repositoryType,
				commitId
			);
		} catch (ApiException exception) {
			if (exception.getErrorCode() == ErrorCode.PROJECT_COMMIT_NOT_FOUND) {
				throw new ApiException(ErrorCode.COMMIT_NOT_FOUND);
			}
			throw exception;
		}
	}

	private QaReportStatus parseReportStatus(String rawStatus) {
		String status = trimToNull(rawStatus);
		if (status == null) {
			return null;
		}

		try {
			return QaReportStatus.valueOf(status.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_QA_REPORT_STATUS);
		}
	}

	private int normalizePage(Integer page) {
		if (page == null) {
			return DEFAULT_PAGE;
		}
		if (page < 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "page must be 0 or greater.");
		}
		return page;
	}

	private int normalizeSize(Integer size) {
		if (size == null) {
			return DEFAULT_SIZE;
		}
		if (size <= 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "size must be greater than 0.");
		}
		return Math.min(size, MAX_SIZE);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
