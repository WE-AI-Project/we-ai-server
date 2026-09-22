package com.weai.server.domain.chat.event;

/**
 * Published after a chat/meeting document upload is persisted. {@code MeetingLibrarySyncListener}
 * (in the project domain) is the sole consumer: it mirrors the uploaded file into the project's
 * Shared Library so teammates who never open the chat still see meeting materials.
 */
public record MeetingFileUploadedEvent(
	Long projectId,
	Long documentId
) {
}
