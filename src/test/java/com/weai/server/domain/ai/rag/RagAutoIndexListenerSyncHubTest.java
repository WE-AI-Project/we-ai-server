package com.weai.server.domain.ai.rag;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.weai.server.domain.ai.rag.event.SyncHubPushedEvent;
import com.weai.server.domain.project.service.ProjectWorkspaceService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class RagAutoIndexListenerSyncHubTest {

	private final ProjectRagIndexService projectRagIndexService = mock(ProjectRagIndexService.class);
	private final WorkspaceRagIndexer workspaceRagIndexer = mock(WorkspaceRagIndexer.class);
	private final ProjectWorkspaceService projectWorkspaceService = mock(ProjectWorkspaceService.class);
	private final RagAutoIndexListener listener = new RagAutoIndexListener(
		projectRagIndexService, workspaceRagIndexer, Runnable::run, projectWorkspaceService
	);

	@Test
	void pushExportsDefaultBranchThenReindexesWorkspace() {
		listener.onSyncHubPushed(new SyncHubPushedEvent(23L));

		InOrder order = inOrder(projectWorkspaceService, workspaceRagIndexer);
		order.verify(projectWorkspaceService).prepareBuildDirectory(23L);
		order.verify(workspaceRagIndexer).schedule(23L);
	}

	@Test
	void exportFailureDoesNotPropagateOrIndex() {
		doThrow(new ApiException(ErrorCode.PROJECT_WORKSPACE_NOT_FOUND))
			.when(projectWorkspaceService).prepareBuildDirectory(anyLong());

		listener.onSyncHubPushed(new SyncHubPushedEvent(23L));

		verify(workspaceRagIndexer, never()).schedule(anyLong());
	}
}
