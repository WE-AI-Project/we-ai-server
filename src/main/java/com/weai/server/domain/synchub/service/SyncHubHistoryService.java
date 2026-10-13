package com.weai.server.domain.synchub.service;

import com.weai.server.domain.project.domain.ProjectRepositoryType;
import com.weai.server.domain.project.request.ProjectGitCommitConventionCheckRequest;
import com.weai.server.domain.project.response.ProjectCommitDetailResponse;
import com.weai.server.domain.project.response.ProjectCommitFileDiffResponse;
import com.weai.server.domain.project.response.ProjectCommitFileListResponse;
import com.weai.server.domain.project.response.ProjectCommitListResponse;
import com.weai.server.domain.project.response.ProjectGitBranchGraphResponse;
import com.weai.server.domain.project.response.ProjectGitCommitConventionCheckResponse;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.synchub.config.SyncHubProperties;
import com.weai.server.domain.synchub.response.SyncHubRepositoryResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.config.AppWebProperties;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only views over a project's SyncHub repository (commit list/detail/files/diff, branch graph,
 * repository info) built with JGit. Response shapes are the ones the client already uses from
 * before the hosted-repository switch; {@code repositoryType} is still accepted but there is a
 * single repository per project, so it only echoes back.
 *
 * A repository that does not exist yet or has no commits is not an error: lists come back empty.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyncHubHistoryService {

	private static final int DEFAULT_BRANCH_GRAPH_LIMIT = 50;
	private static final int MAX_BRANCH_GRAPH_LIMIT = 200;
	private static final int SHORT_HASH_LENGTH = 7;
	private static final int MAX_BRANCH_CONTAINS_WALK = 20_000;
	private static final Pattern COMMIT_HASH_PATTERN = Pattern.compile("^[0-9a-fA-F]{4,40}$");
	private static final Set<String> COMMIT_TYPES = Set.of(
		"feat", "fix", "docs", "style", "refactor", "test", "chore", "build", "ci", "perf", "revert"
	);
	private static final Pattern CONVENTIONAL_COMMIT_PATTERN = Pattern.compile(
		"^(?<type>[a-zA-Z]+)(?:\\((?<scope>[^()\\s]+)\\))?(?<breaking>!)?:\\s*(?<subject>.*)$"
	);

	private final ProjectService projectService;
	private final UserService userService;
	private final SyncHubRepositoryService syncHubRepositoryService;
	private final SyncHubProperties syncHubProperties;
	private final AppWebProperties appWebProperties;

	public SyncHubRepositoryResponse getRepositoryInfo(String userEmail, Long projectId) {
		validateAccess(userEmail, projectId);
		String remoteUrl = buildRemoteUrl(projectId);
		String remoteName = syncHubProperties.getRemoteName();

		Optional<Repository> opened = syncHubRepositoryService.openIfExists(projectId);
		if (opened.isEmpty()) {
			return new SyncHubRepositoryResponse(
				projectId, remoteUrl, remoteName, false, true, syncHubProperties.getDefaultBranch(), null, List.of()
			);
		}

		try (Repository repository = opened.get()) {
			List<String> branches = syncHubRepositoryService.listBranches(repository).stream()
				.map(ref -> Repository.shortenRefName(ref.getName()))
				.toList();
			String headCommitHash = syncHubRepositoryService.resolveDefaultBranch(repository)
				.map(ref -> ref.getObjectId().name())
				.orElse(null);
			return new SyncHubRepositoryResponse(
				projectId,
				remoteUrl,
				remoteName,
				true,
				headCommitHash == null,
				syncHubRepositoryService.resolveDefaultBranchName(repository),
				headCommitHash,
				branches
			);
		}
	}

	public ProjectCommitListResponse getProjectCommits(
		String userEmail,
		Long projectId,
		String rawRepositoryType,
		Integer rawLimit
	) {
		ProjectRepositoryType repositoryType = resolveRepositoryType(rawRepositoryType);
		int limit = normalizeLimit(rawLimit);
		validateAccess(userEmail, projectId);

		Optional<Repository> opened = syncHubRepositoryService.openIfExists(projectId);
		if (opened.isEmpty()) {
			return new ProjectCommitListResponse(projectId, repositoryType, limit, List.of());
		}

		try (Repository repository = opened.get()) {
			Optional<Ref> defaultBranch = syncHubRepositoryService.resolveDefaultBranch(repository);
			if (defaultBranch.isEmpty()) {
				return new ProjectCommitListResponse(projectId, repositoryType, limit, List.of());
			}

			List<ProjectCommitListResponse.CommitSummaryResponse> commits = new ArrayList<>();
			try (RevWalk revWalk = new RevWalk(repository); ObjectReader reader = repository.newObjectReader()) {
				revWalk.markStart(revWalk.parseCommit(defaultBranch.get().getObjectId()));
				for (RevCommit commit : revWalk) {
					if (commits.size() >= limit) {
						break;
					}
					List<CommitFile> files = readCommitFiles(repository, revWalk, commit, false);
					commits.add(new ProjectCommitListResponse.CommitSummaryResponse(
						commit.getName(),
						abbreviate(reader, commit),
						commit.getShortMessage(),
						commit.getAuthorIdent().getName(),
						commit.getAuthorIdent().getEmailAddress(),
						toLocalDateTime(commit.getAuthorIdent()),
						files.size(),
						files.stream().mapToLong(CommitFile::additions).sum(),
						files.stream().mapToLong(CommitFile::deletions).sum()
					));
				}
			}
			return new ProjectCommitListResponse(projectId, repositoryType, limit, commits);
		} catch (IOException exception) {
			throw readFailure(exception);
		}
	}

	public ProjectCommitDetailResponse getProjectCommitDetail(
		String userEmail,
		Long projectId,
		String rawRepositoryType,
		String commitHash
	) {
		ProjectRepositoryType repositoryType = resolveRepositoryType(rawRepositoryType);
		validateAccess(userEmail, projectId);

		try (Repository repository = openForCommit(projectId);
			RevWalk revWalk = new RevWalk(repository);
			ObjectReader reader = repository.newObjectReader()) {
			RevCommit commit = parseCommit(repository, revWalk, commitHash);
			List<CommitFile> files = readCommitFiles(repository, revWalk, commit, false);
			return new ProjectCommitDetailResponse(
				projectId,
				repositoryType,
				commit.getName(),
				abbreviate(reader, commit),
				commit.getShortMessage(),
				extractBody(commit.getFullMessage()),
				commit.getAuthorIdent().getName(),
				commit.getAuthorIdent().getEmailAddress(),
				toLocalDateTime(commit.getAuthorIdent()),
				files.size(),
				files.stream().mapToLong(CommitFile::additions).sum(),
				files.stream().mapToLong(CommitFile::deletions).sum()
			);
		} catch (IOException exception) {
			throw readFailure(exception);
		}
	}

	public ProjectCommitFileListResponse getProjectCommitFiles(
		String userEmail,
		Long projectId,
		String rawRepositoryType,
		String commitHash
	) {
		ProjectRepositoryType repositoryType = resolveRepositoryType(rawRepositoryType);
		validateAccess(userEmail, projectId);

		try (Repository repository = openForCommit(projectId); RevWalk revWalk = new RevWalk(repository)) {
			RevCommit commit = parseCommit(repository, revWalk, commitHash);
			List<CommitFile> files = readCommitFiles(repository, revWalk, commit, false);
			return new ProjectCommitFileListResponse(
				projectId,
				repositoryType,
				commitHash,
				files.stream()
					.map(file -> new ProjectCommitFileListResponse.CommitFileResponse(
						file.path(),
						file.fileName(),
						file.extension(),
						file.status(),
						file.additions(),
						file.deletions()
					))
					.toList()
			);
		} catch (IOException exception) {
			throw readFailure(exception);
		}
	}

	public ProjectCommitFileDiffResponse getProjectCommitFileDiff(
		String userEmail,
		Long projectId,
		String rawRepositoryType,
		String commitHash,
		String filePath
	) {
		if (filePath == null || filePath.isBlank()) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "filePath is required.");
		}

		ProjectRepositoryType repositoryType = resolveRepositoryType(rawRepositoryType);
		validateAccess(userEmail, projectId);

		try (Repository repository = openForCommit(projectId); RevWalk revWalk = new RevWalk(repository)) {
			RevCommit commit = parseCommit(repository, revWalk, commitHash);
			CommitFile file = readCommitFiles(repository, revWalk, commit, true).stream()
				.filter(candidate -> candidate.path().equals(filePath))
				.findFirst()
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_COMMIT_FILE_NOT_FOUND));

			return new ProjectCommitFileDiffResponse(
				projectId,
				repositoryType,
				commitHash,
				file.path(),
				file.fileName(),
				file.extension(),
				file.status(),
				file.additions(),
				file.deletions(),
				file.diff()
			);
		} catch (IOException exception) {
			throw readFailure(exception);
		}
	}

	public ProjectGitBranchGraphResponse getBranchGraph(
		String userEmail,
		Long projectId,
		String rawBranch,
		Integer rawMaxCount,
		boolean includeRemote
	) {
		validateAccess(userEmail, projectId);
		int maxCount = normalizeBranchGraphLimit(rawMaxCount);
		String branch = validateBranchName(rawBranch);

		Optional<Repository> opened = syncHubRepositoryService.openIfExists(projectId);
		if (opened.isEmpty()) {
			return new ProjectGitBranchGraphResponse(projectId, syncHubProperties.getDefaultBranch(), List.of(), List.of(), List.of());
		}

		// A bare hosted repository has no remote-tracking refs, so includeRemote has nothing to add.
		try (Repository repository = opened.get();
			RevWalk revWalk = new RevWalk(repository);
			ObjectReader reader = repository.newObjectReader()) {
			String currentBranch = syncHubRepositoryService.resolveDefaultBranchName(repository);
			List<Ref> branchRefs = syncHubRepositoryService.listBranches(repository);
			if (branchRefs.isEmpty()) {
				return new ProjectGitBranchGraphResponse(projectId, currentBranch, List.of(), List.of(), List.of());
			}

			Map<String, RevCommit> branchHeads = new LinkedHashMap<>();
			for (Ref ref : branchRefs) {
				branchHeads.put(Repository.shortenRefName(ref.getName()), revWalk.parseCommit(ref.getObjectId()));
			}
			if (branch != null && !branchHeads.containsKey(branch)) {
				throw new ApiException(ErrorCode.GIT_BRANCH_NOT_FOUND);
			}

			revWalk.sort(RevSort.COMMIT_TIME_DESC);
			if (branch == null) {
				for (RevCommit head : branchHeads.values()) {
					revWalk.markStart(head);
				}
			} else {
				revWalk.markStart(branchHeads.get(branch));
			}
			List<RevCommit> commits = new ArrayList<>();
			for (RevCommit commit : revWalk) {
				if (commits.size() >= maxCount) {
					break;
				}
				commits.add(commit);
			}

			Set<ObjectId> nodeIds = new HashSet<>(commits);
			Map<ObjectId, List<String>> containingBranches = findContainingBranches(repository, branchHeads, nodeIds);

			List<ProjectGitBranchGraphResponse.CommitNodeResponse> nodes = new ArrayList<>();
			for (int index = 0; index < commits.size(); index++) {
				RevCommit commit = commits.get(index);
				nodes.add(new ProjectGitBranchGraphResponse.CommitNodeResponse(
					commit.getName(),
					abbreviate(reader, commit),
					commit.getShortMessage(),
					commit.getAuthorIdent().getName(),
					commit.getAuthorIdent().getEmailAddress(),
					toLocalDateTime(commit.getAuthorIdent()),
					containingBranches.getOrDefault(commit, List.of()),
					null,
					index
				));
			}

			List<ProjectGitBranchGraphResponse.CommitEdgeResponse> edges = new ArrayList<>();
			for (RevCommit commit : commits) {
				for (RevCommit parent : commit.getParents()) {
					if (nodeIds.contains(parent)) {
						edges.add(new ProjectGitBranchGraphResponse.CommitEdgeResponse(parent.getName(), commit.getName(), "PARENT"));
					}
				}
			}

			List<ProjectGitBranchGraphResponse.BranchResponse> branches = new ArrayList<>();
			for (Map.Entry<String, RevCommit> entry : branchHeads.entrySet()) {
				branches.add(new ProjectGitBranchGraphResponse.BranchResponse(
					entry.getKey(),
					entry.getKey().equals(currentBranch),
					false,
					entry.getValue().getName(),
					entry.getValue().getShortMessage()
				));
			}

			return new ProjectGitBranchGraphResponse(projectId, currentBranch, branches, nodes, edges);
		} catch (IOException exception) {
			throw readFailure(exception);
		}
	}

	public ProjectGitCommitConventionCheckResponse checkCommitConvention(
		String userEmail,
		Long projectId,
		ProjectGitCommitConventionCheckRequest request
	) {
		validateAccess(userEmail, projectId);
		String message = validateCommitMessage(request == null ? null : request.message());
		validateCommitDescription(request == null ? null : request.description());

		Matcher matcher = CONVENTIONAL_COMMIT_PATTERN.matcher(message);
		List<ProjectGitCommitConventionCheckResponse.ConventionIssue> errors = new ArrayList<>();
		List<ProjectGitCommitConventionCheckResponse.ConventionIssue> warnings = new ArrayList<>();
		String type = null;
		String scope = null;
		String subject = null;
		String normalizedMessage = null;

		if (!matcher.matches()) {
			errors.add(conventionIssue(
				"INVALID_COMMIT_MESSAGE_FORMAT",
				"커밋 메시지는 'type: subject' 형식이어야 합니다."
			));
		} else {
			type = matcher.group("type").toLowerCase(Locale.ROOT);
			scope = trimToNull(matcher.group("scope"));
			subject = trimToNull(matcher.group("subject"));
			boolean breaking = matcher.group("breaking") != null;

			if (!COMMIT_TYPES.contains(type)) {
				errors.add(conventionIssue("UNSUPPORTED_COMMIT_TYPE", "허용되지 않은 커밋 타입입니다."));
			}
			if (subject == null) {
				errors.add(conventionIssue("COMMIT_SUBJECT_REQUIRED", "콜론(:) 뒤에 커밋 제목을 입력해주세요."));
			} else {
				if (subject.length() > 72) {
					warnings.add(conventionIssue("COMMIT_SUBJECT_TOO_LONG", "커밋 제목은 72자 이하를 권장합니다."));
				}
				if (subject.endsWith(".")) {
					warnings.add(conventionIssue("COMMIT_SUBJECT_ENDS_WITH_PERIOD", "커밋 제목 끝의 마침표는 생략하는 것을 권장합니다."));
				}
			}

			if (errors.isEmpty()) {
				normalizedMessage = type
					+ (scope == null ? "" : "(" + scope + ")")
					+ (breaking ? "!" : "")
					+ ": " + subject;
			}
		}

		List<String> suggestions = errors.isEmpty()
			? List.of("좋은 커밋 메시지입니다.")
			: List.of("예: feat: 채팅방 생성 API 구현", "예: fix(auth): 토큰 재발급 오류 수정");
		return new ProjectGitCommitConventionCheckResponse(
			errors.isEmpty(), type, scope, subject, normalizedMessage, errors, warnings, suggestions
		);
	}

	private void validateAccess(String userEmail, Long projectId) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());
	}

	private String buildRemoteUrl(Long projectId) {
		String baseUrl = appWebProperties.getBaseUrl().trim();
		while (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl + "/api/synchub/" + projectId + ".git";
	}

	// repositoryType은 클라이언트 호환을 위해 받기만 한다 - 프로젝트당 저장소가 하나라 잘못된 값도 거절하지 않는다.
	private ProjectRepositoryType resolveRepositoryType(String rawRepositoryType) {
		try {
			return ProjectRepositoryType.from(rawRepositoryType);
		} catch (ApiException exception) {
			return ProjectRepositoryType.BACKEND;
		}
	}

	private Repository openForCommit(Long projectId) {
		return syncHubRepositoryService.openIfExists(projectId)
			.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_COMMIT_NOT_FOUND));
	}

	private RevCommit parseCommit(Repository repository, RevWalk revWalk, String rawCommitHash) throws IOException {
		String commitHash = rawCommitHash == null ? "" : rawCommitHash.trim();
		if (!COMMIT_HASH_PATTERN.matcher(commitHash).matches()) {
			throw new ApiException(ErrorCode.PROJECT_COMMIT_NOT_FOUND);
		}

		try (ObjectReader reader = repository.newObjectReader()) {
			ObjectId objectId;
			if (commitHash.length() == org.eclipse.jgit.lib.Constants.OBJECT_ID_STRING_LENGTH) {
				objectId = ObjectId.fromString(commitHash);
			} else {
				var candidates = reader.resolve(AbbreviatedObjectId.fromString(commitHash));
				if (candidates.size() != 1) {
					throw new ApiException(ErrorCode.PROJECT_COMMIT_NOT_FOUND);
				}
				objectId = candidates.iterator().next();
			}
			return revWalk.parseCommit(objectId);
		} catch (MissingObjectException | org.eclipse.jgit.errors.IncorrectObjectTypeException exception) {
			throw new ApiException(ErrorCode.PROJECT_COMMIT_NOT_FOUND);
		}
	}

	private List<CommitFile> readCommitFiles(Repository repository, RevWalk revWalk, RevCommit commit, boolean includeDiffText)
		throws IOException {
		ByteArrayOutputStream diffOutput = new ByteArrayOutputStream();
		try (ObjectReader reader = repository.newObjectReader();
			DiffFormatter formatter = new DiffFormatter(includeDiffText ? diffOutput : DisabledOutputStream.INSTANCE)) {
			formatter.setRepository(repository);
			formatter.setDiffComparator(RawTextComparator.DEFAULT);
			formatter.setDetectRenames(true);
			formatter.setContext(3);

			AbstractTreeIterator oldTree;
			if (commit.getParentCount() > 0) {
				RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
				CanonicalTreeParser parser = new CanonicalTreeParser();
				parser.reset(reader, parent.getTree());
				oldTree = parser;
			} else {
				oldTree = new EmptyTreeIterator();
			}
			CanonicalTreeParser newTree = new CanonicalTreeParser();
			newTree.reset(reader, commit.getTree());

			List<CommitFile> files = new ArrayList<>();
			for (DiffEntry entry : formatter.scan(oldTree, newTree)) {
				long additions = 0L;
				long deletions = 0L;
				for (Edit edit : formatter.toFileHeader(entry).toEditList()) {
					additions += edit.getLengthB();
					deletions += edit.getLengthA();
				}

				String diffText = "";
				if (includeDiffText) {
					diffOutput.reset();
					formatter.format(entry);
					formatter.flush();
					diffText = diffOutput.toString(StandardCharsets.UTF_8).trim();
				}
				files.add(CommitFile.of(entry, additions, deletions, diffText));
			}
			return files;
		}
	}

	/**
	 * For each graph node, the branches whose history contains it ({@code git branch --contains}).
	 * One walk per branch instead of one per (node, branch) pair; each walk stops as soon as every
	 * node has been seen, and is capped for very long histories.
	 */
	private Map<ObjectId, List<String>> findContainingBranches(
		Repository repository,
		Map<String, RevCommit> branchHeads,
		Set<ObjectId> nodeIds
	) throws IOException {
		Map<ObjectId, List<String>> result = new HashMap<>();
		for (Map.Entry<String, RevCommit> entry : branchHeads.entrySet()) {
			try (RevWalk walk = new RevWalk(repository)) {
				walk.markStart(walk.parseCommit(entry.getValue()));
				int seen = 0;
				int visited = 0;
				for (RevCommit commit : walk) {
					if (++visited > MAX_BRANCH_CONTAINS_WALK) {
						break;
					}
					if (nodeIds.contains(commit)) {
						result.computeIfAbsent(commit.copy(), ignored -> new ArrayList<>()).add(entry.getKey());
						if (++seen == nodeIds.size()) {
							break;
						}
					}
				}
			}
		}
		return result;
	}

	private int normalizeLimit(Integer rawLimit) {
		int fallbackLimit = syncHubProperties.getDefaultCommitLimit() > 0 ? syncHubProperties.getDefaultCommitLimit() : 20;
		int maxLimit = syncHubProperties.getMaxCommitLimit() > 0 ? syncHubProperties.getMaxCommitLimit() : 100;
		int limit = rawLimit == null ? fallbackLimit : rawLimit;
		if (limit <= 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "limit must be greater than 0.");
		}
		return Math.min(limit, maxLimit);
	}

	private int normalizeBranchGraphLimit(Integer rawMaxCount) {
		int maxCount = rawMaxCount == null ? DEFAULT_BRANCH_GRAPH_LIMIT : rawMaxCount;
		if (maxCount < 1 || maxCount > MAX_BRANCH_GRAPH_LIMIT) {
			throw new ApiException(ErrorCode.INVALID_GIT_GRAPH_LIMIT);
		}
		return maxCount;
	}

	private String validateBranchName(String rawBranch) {
		String branch = trimToNull(rawBranch);
		if (branch == null) {
			return null;
		}
		if (!branch.matches("[\\p{L}\\p{N}._/-]+")
			|| branch.startsWith("-")
			|| branch.contains("..")
			|| branch.contains("//")
			|| branch.endsWith("/")
			|| branch.endsWith(".")
			|| branch.endsWith(".lock")) {
			throw new ApiException(ErrorCode.INVALID_GIT_BRANCH_NAME);
		}
		return branch;
	}

	private String validateCommitMessage(String rawMessage) {
		String message = trimToNull(rawMessage);
		if (message == null) {
			throw new ApiException(ErrorCode.GIT_COMMIT_MESSAGE_REQUIRED);
		}
		if (message.length() > 200) {
			throw new ApiException(ErrorCode.GIT_COMMIT_MESSAGE_TOO_LONG);
		}
		return message;
	}

	private void validateCommitDescription(String rawDescription) {
		String description = trimToNull(rawDescription);
		if (description != null && description.length() > 1000) {
			throw new ApiException(ErrorCode.GIT_COMMIT_DESCRIPTION_TOO_LONG);
		}
	}

	private ProjectGitCommitConventionCheckResponse.ConventionIssue conventionIssue(String code, String message) {
		return new ProjectGitCommitConventionCheckResponse.ConventionIssue("message", code, message);
	}

	private String abbreviate(ObjectReader reader, ObjectId objectId) {
		try {
			return reader.abbreviate(objectId, SHORT_HASH_LENGTH).name();
		} catch (IOException exception) {
			return objectId.name().substring(0, SHORT_HASH_LENGTH);
		}
	}

	// git의 %ad(iso-strict)를 LocalDateTime으로 바꾸던 기존 동작과 같게, 작성자 타임존 기준 시각을 쓴다.
	private LocalDateTime toLocalDateTime(PersonIdent ident) {
		return LocalDateTime.ofInstant(ident.getWhenAsInstant(), ident.getZoneId());
	}

	// git의 %b와 같다: 첫 문단(제목) 뒤의 나머지 본문.
	private String extractBody(String fullMessage) {
		if (fullMessage == null) {
			return "";
		}
		String normalized = fullMessage.replace("\r\n", "\n");
		int paragraphBreak = normalized.indexOf("\n\n");
		return paragraphBreak < 0 ? "" : normalized.substring(paragraphBreak + 2).trim();
	}

	private ApiException readFailure(IOException exception) {
		return new ApiException(ErrorCode.SYNCHUB_REPOSITORY_READ_FAILED, exception.getMessage(), exception);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record CommitFile(
		String path,
		String fileName,
		String extension,
		String status,
		long additions,
		long deletions,
		String diff
	) {

		// 클라이언트 타입은 ADDED/MODIFIED/DELETED뿐이라, rename/copy는 예전 git CLI 파서와 같이 MODIFIED(새 경로)로 내려준다.
		private static CommitFile of(DiffEntry entry, long additions, long deletions, String diff) {
			String status = switch (entry.getChangeType()) {
				case ADD -> "ADDED";
				case DELETE -> "DELETED";
				default -> "MODIFIED";
			};
			String path = entry.getChangeType() == DiffEntry.ChangeType.DELETE ? entry.getOldPath() : entry.getNewPath();
			String normalizedPath = path == null ? "" : path.replace('\\', '/');
			String fileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
			String extension = fileName.contains(".")
				? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
				: "";
			return new CommitFile(normalizedPath, fileName, extension, status, additions, deletions, diff);
		}
	}
}
