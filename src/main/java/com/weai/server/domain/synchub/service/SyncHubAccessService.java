package com.weai.server.domain.synchub.service;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.domain.ProjectMemberRole;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.domain.ProjectStatus;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Membership/role check for Git Smart HTTP requests. Fetch/clone: any ACTIVE member of an ACTIVE
 * project. Push: LEADER or MEMBER (GUEST is read-only).
 *
 * Unlike the JSON APIs this does not throw {@code ApiException}: the Git servlet translates the
 * outcome into the HTTP status the git client understands (401/403/404).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyncHubAccessService {

	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;

	public SyncHubAccess check(String principalEmail, Long projectId) {
		User user = principalEmail == null ? null : userRepository.findByEmail(principalEmail).orElse(null);
		if (user == null) {
			return SyncHubAccess.denied(projectId, SyncHubAccess.Outcome.UNAUTHENTICATED);
		}

		Project project = projectRepository.findById(projectId).orElse(null);
		if (project == null || project.getStatus() != ProjectStatus.ACTIVE) {
			return SyncHubAccess.denied(projectId, SyncHubAccess.Outcome.PROJECT_NOT_FOUND);
		}

		return projectMemberRepository.findByProject_IdAndUser_IdAndStatus(projectId, user.getId(), ProjectMemberStatus.ACTIVE)
			.map(ProjectMember::getRole)
			.map(role -> new SyncHubAccess(projectId, user.getId(), role, SyncHubAccess.Outcome.GRANTED))
			.orElseGet(() -> SyncHubAccess.denied(projectId, SyncHubAccess.Outcome.NOT_A_MEMBER));
	}

	public record SyncHubAccess(Long projectId, Long userId, ProjectMemberRole role, Outcome outcome) {

		public enum Outcome {
			GRANTED,
			UNAUTHENTICATED,
			PROJECT_NOT_FOUND,
			NOT_A_MEMBER
		}

		static SyncHubAccess denied(Long projectId, Outcome outcome) {
			return new SyncHubAccess(projectId, null, null, outcome);
		}

		public boolean canRead() {
			return outcome == Outcome.GRANTED;
		}

		public boolean canPush() {
			return canRead() && (role == ProjectMemberRole.LEADER || role == ProjectMemberRole.MEMBER);
		}
	}
}
