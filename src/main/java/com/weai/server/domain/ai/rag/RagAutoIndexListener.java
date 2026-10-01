package com.weai.server.domain.ai.rag;

import com.weai.server.domain.ai.rag.event.RagIndexRequestedEvent;
import com.weai.server.domain.ai.rag.event.WorkspaceSnapshotUploadedEvent;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Automatic RAG indexing. Runs only after the publishing transaction commits (so a rolled-back
 * upload is never indexed) and off the request thread (embedding goes over the network to Ollama),
 * so a slow or failing index never delays or breaks the upload/meeting request that triggered it.
 */
@Slf4j
@Component
public class RagAutoIndexListener {

	private final ProjectRagIndexService projectRagIndexService;
	private final WorkspaceRagIndexer workspaceRagIndexer;
	private final Executor ragIndexExecutor;

	public RagAutoIndexListener(
		ProjectRagIndexService projectRagIndexService,
		WorkspaceRagIndexer workspaceRagIndexer,
		@Qualifier("ragIndexExecutor") Executor ragIndexExecutor
	) {
		this.projectRagIndexService = projectRagIndexService;
		this.workspaceRagIndexer = workspaceRagIndexer;
		this.ragIndexExecutor = ragIndexExecutor;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onIndexRequested(RagIndexRequestedEvent event) {
		try {
			ragIndexExecutor.execute(() -> {
				try {
					projectRagIndexService.index(event.projectId(), event.source(), event.text(), event.origin());
				} catch (RuntimeException exception) {
					log.warn("Auto RAG indexing failed for projectId={}, source={}", event.projectId(), event.source(), exception);
				}
			});
		} catch (TaskRejectedException exception) {
			log.warn("Auto RAG indexing skipped for projectId={}, source={}: executor queue full.", event.projectId(), event.source());
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onWorkspaceUploaded(WorkspaceSnapshotUploadedEvent event) {
		workspaceRagIndexer.schedule(event.projectId());
	}
}
