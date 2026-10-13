package com.weai.server.domain.ai.rag.event;

/** Published after a push to a project's SyncHub repository has been accepted. */
public record SyncHubPushedEvent(Long projectId) {
}
