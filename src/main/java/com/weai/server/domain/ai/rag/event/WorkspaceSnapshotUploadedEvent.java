package com.weai.server.domain.ai.rag.event;

/** Published after a project workspace snapshot has been extracted on the server. */
public record WorkspaceSnapshotUploadedEvent(Long projectId) {
}
