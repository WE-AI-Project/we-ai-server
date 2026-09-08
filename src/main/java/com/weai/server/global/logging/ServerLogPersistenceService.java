package com.weai.server.global.logging;

import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ServerLog;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.project.repository.ServerLogRepository;
import com.weai.server.domain.project.service.ServerLogStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServerLogPersistenceService {

	private final ProjectRepository projectRepository;
	private final ServerLogRepository serverLogRepository;
	private final ServerLogStreamService serverLogStreamService;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void persist(ServerLogCaptureEvent event) {
		Project project = projectRepository.findById(event.projectId()).orElse(null);
		if (project == null) {
			return;
		}

		ServerLog serverLog = serverLogRepository.save(ServerLog.create(
			project,
			event.level(),
			event.source(),
			event.message(),
			event.threadName(),
			event.loggerName(),
			event.traceId()
		));
		serverLogStreamService.publish(serverLog);
	}
}
