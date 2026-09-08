package com.weai.server.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weai.server.domain.project.repository.ServerLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServerLogRetentionServiceTest {

	@Test
	void physicallyDeletesLogsSoftDeletedMoreThanThirtyDaysAgo() {
		ServerLogRepository repository = mock(ServerLogRepository.class);
		ServerLogRetentionService service = new ServerLogRetentionService(repository, 30);
		when(repository.deleteAllSoftDeletedBefore(org.mockito.ArgumentMatchers.any())).thenReturn(5);
		LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(30);

		int deletedCount = service.purgeExpiredSoftDeletedLogs();

		ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(repository).deleteAllSoftDeletedBefore(cutoffCaptor.capture());
		assertThat(deletedCount).isEqualTo(5);
		assertThat(cutoffCaptor.getValue()).isBetween(expectedCutoff.minusSeconds(1), expectedCutoff.plusSeconds(1));
	}
}
