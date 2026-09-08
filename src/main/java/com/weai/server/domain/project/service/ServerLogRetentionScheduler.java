package com.weai.server.domain.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ServerLogRetentionScheduler {

	private static final Logger log = LoggerFactory.getLogger(ServerLogRetentionScheduler.class);

	private final ServerLogRetentionService retentionService;

	public ServerLogRetentionScheduler(ServerLogRetentionService retentionService) {
		this.retentionService = retentionService;
	}

	@Scheduled(
		cron = "${server-log.retention.cleanup-cron:0 0 3 * * *}",
		zone = "${server-log.retention.cleanup-zone:Asia/Seoul}"
	)
	public void purgeExpiredSoftDeletedLogs() {
		int deletedCount = retentionService.purgeExpiredSoftDeletedLogs();
		if (deletedCount > 0) {
			log.info("Physically deleted {} server log(s) after the retention period.", deletedCount);
		}
	}
}
