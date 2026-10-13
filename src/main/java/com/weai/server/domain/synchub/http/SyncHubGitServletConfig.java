package com.weai.server.domain.synchub.http;

import com.weai.server.domain.ai.rag.event.SyncHubPushedEvent;
import com.weai.server.domain.synchub.config.SyncHubProperties;
import com.weai.server.domain.synchub.service.SyncHubAccessService;
import com.weai.server.domain.synchub.service.SyncHubAccessService.SyncHubAccess;
import com.weai.server.domain.synchub.service.SyncHubRepositoryService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.UploadPack;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Serves every project's SyncHub repository over Git Smart HTTP with JGit's {@link GitServlet}:
 * <pre>
 * GET  /api/synchub/{projectId}.git/info/refs?service=git-upload-pack|git-receive-pack
 * POST /api/synchub/{projectId}.git/git-upload-pack
 * POST /api/synchub/{projectId}.git/git-receive-pack
 * </pre>
 * It is a plain servlet (not a Spring MVC controller), so the ApiResponse envelope and
 * GlobalExceptionHandler never wrap the binary protocol. Authentication (Bearer JWT or HTTP Basic)
 * is done by the dedicated Spring Security chain in {@code SecurityConfig}; this class only checks
 * project membership/role on the already-authenticated principal.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SyncHubProperties.class)
public class SyncHubGitServletConfig {

	public static final String URL_PREFIX = "/api/synchub";

	private static final String ACCESS_ATTRIBUTE = SyncHubGitServletConfig.class.getName() + ".access";
	private static final Pattern REPOSITORY_NAME = Pattern.compile("^(?<projectId>\\d{1,18})(?:\\.git)?/?$");

	@Bean
	public ServletRegistrationBean<GitServlet> syncHubGitServlet(
		SyncHubRepositoryService syncHubRepositoryService,
		SyncHubAccessService syncHubAccessService,
		SyncHubProperties syncHubProperties,
		ApplicationEventPublisher eventPublisher
	) {
		GitServlet gitServlet = new GitServlet();
		// Smart protocol only: the "dumb" file-serving fallback would bypass the push role check.
		gitServlet.setAsIsFileService(org.eclipse.jgit.http.server.resolver.AsIsFileService.DISABLED);
		gitServlet.setRepositoryResolver((request, name) -> {
			Long projectId = parseProjectId(name);
			SyncHubAccess access = syncHubAccessService.check(currentPrincipalName(), projectId);
			switch (access.outcome()) {
				case UNAUTHENTICATED -> throw new ServiceNotAuthorizedException();
				case PROJECT_NOT_FOUND -> throw new RepositoryNotFoundException(name);
				case NOT_A_MEMBER -> throw new ServiceNotEnabledException("You are not an active member of this project.");
				case GRANTED -> {
				}
			}
			request.setAttribute(ACCESS_ATTRIBUTE, access);
			return syncHubRepositoryService.openOrCreate(projectId);
		});
		gitServlet.setUploadPackFactory((request, repository) -> new UploadPack(repository));
		gitServlet.setReceivePackFactory((request, repository) -> {
			SyncHubAccess access = (SyncHubAccess) request.getAttribute(ACCESS_ATTRIBUTE);
			if (access == null || !access.canPush()) {
				throw new ServiceNotEnabledException("Push requires the LEADER or MEMBER role (GUEST is read-only).");
			}
			return createReceivePack(repository, access, syncHubRepositoryService, syncHubProperties, eventPublisher);
		});

		ServletRegistrationBean<GitServlet> registration = new ServletRegistrationBean<>(gitServlet, URL_PREFIX + "/*");
		registration.setName("syncHubGitServlet");
		registration.setLoadOnStartup(1);
		return registration;
	}

	private ReceivePack createReceivePack(
		Repository repository,
		SyncHubAccess access,
		SyncHubRepositoryService syncHubRepositoryService,
		SyncHubProperties syncHubProperties,
		ApplicationEventPublisher eventPublisher
	) {
		ReceivePack receivePack = new ReceivePack(repository);
		String principal = currentPrincipalName();
		receivePack.setRefLogIdent(new PersonIdent(principal == null ? "synchub" : principal, principal == null ? "" : principal));
		// fsck incoming objects: rejects malformed trees (e.g. "..", ".git" entries) before they are stored.
		receivePack.setCheckReceivedObjects(true);
		receivePack.setMaxPackSizeLimit(syncHubProperties.getMaxPushSizeBytes());
		receivePack.setPostReceiveHook((pack, commands) -> {
			syncHubRepositoryService.repairDanglingHead(repository);
			for (ReceiveCommand command : commands) {
				log.info(
					"SyncHub push projectId={} userId={} ref={} {} -> {} result={}",
					access.projectId(), access.userId(), command.getRefName(),
					command.getOldId().name(), command.getNewId().name(), command.getResult()
				);
			}
			boolean branchUpdated = commands.stream().anyMatch(command ->
				command.getResult() == ReceiveCommand.Result.OK && command.getRefName().startsWith("refs/heads/"));
			if (branchUpdated) {
				// 기본 브랜치를 워크스페이스로 내보내고 RAG 재색인 (비동기 - push 응답을 지연시키지 않는다)
				eventPublisher.publishEvent(new SyncHubPushedEvent(access.projectId()));
			}
		});
		return receivePack;
	}

	static Long parseProjectId(String repositoryName) throws RepositoryNotFoundException {
		Matcher matcher = REPOSITORY_NAME.matcher(repositoryName == null ? "" : repositoryName);
		if (!matcher.matches()) {
			throw new RepositoryNotFoundException(String.valueOf(repositoryName));
		}
		return Long.valueOf(matcher.group("projectId"));
	}

	private static String currentPrincipalName() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null || !authentication.isAuthenticated() ? null : authentication.getName();
	}
}
