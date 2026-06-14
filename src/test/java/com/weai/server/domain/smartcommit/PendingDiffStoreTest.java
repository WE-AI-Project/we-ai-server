package com.weai.server.domain.smartcommit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PendingDiffStoreTest {

	@Test
	void pendingDiffsAreDrainedPerProject() {
		PendingDiffStore store = new PendingDiffStore();
		store.addOrUpdate(1L, "src/App.java", "project-one-diff");
		store.addOrUpdate(2L, "src/App.java", "project-two-diff");

		List<PendingDiffStore.PendingCommitBatch> batches = store.drainAllReadyForAutoCommit(
			Instant.now().plusSeconds(1),
			Duration.ZERO,
			Duration.ZERO
		);

		assertThat(batches).hasSize(2);
		assertThat(batches).extracting(PendingDiffStore.PendingCommitBatch::projectId)
			.containsExactly(1L, 2L);
		assertThat(batches.get(0).combinedDiff()).contains("project-one-diff").doesNotContain("project-two-diff");
		assertThat(batches.get(1).combinedDiff()).contains("project-two-diff").doesNotContain("project-one-diff");
	}

	@Test
	void failedBatchRestoresOnlyItsProject() {
		PendingDiffStore store = new PendingDiffStore();
		store.addOrUpdate(1L, "a.txt", "old-diff");
		PendingDiffStore.PendingCommitBatch batch = store.drainForManualCommit(
			1L,
			Instant.now(),
			Duration.ZERO
		).orElseThrow();

		store.addOrUpdate(1L, "a.txt", "new-diff");
		store.addOrUpdate(2L, "a.txt", "other-project-diff");
		store.restore(batch);

		assertThat(store.snapshot(1L).pendingFileCount()).isEqualTo(1);
		assertThat(store.snapshot(2L).pendingFileCount()).isEqualTo(1);
		PendingDiffStore.PendingCommitBatch restored = store.drainForManualCommit(
			1L,
			Instant.now().plusSeconds(1),
			Duration.ZERO
		).orElseThrow();
		assertThat(restored.combinedDiff()).contains("old-diff", "new-diff").doesNotContain("other-project-diff");
	}
}
