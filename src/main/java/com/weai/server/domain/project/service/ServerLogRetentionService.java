package com.weai.server.domain.project.service;

import com.weai.server.domain.project.repository.ServerLogRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServerLogRetentionService {

	private final ServerLogRepository serverLogRepository;
	private final int retentionDays;

	public ServerLogRetentionService(
		ServerLogRepository serverLogRepository,
		@Value("${server-log.retention.days:30}") int retentionDays
	) {
		this.serverLogRepository = serverLogRepository;
		this.retentionDays = Math.max(1, retentionDays);
	}

	@Transactional
	public int purgeExpiredSoftDeletedLogs() {
		return serverLogRepository.deleteAllSoftDeletedBefore(LocalDateTime.now().minusDays(retentionDays));
	}
}
