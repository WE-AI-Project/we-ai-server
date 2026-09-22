package com.weai.server.domain.project.event;

import com.weai.server.domain.chat.domain.ChatDocument;
import com.weai.server.domain.chat.event.MeetingFileUploadedEvent;
import com.weai.server.domain.chat.repository.ChatDocumentRepository;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectLibraryResource;
import com.weai.server.domain.project.repository.ProjectLibraryResourceRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link MeetingFileUploadedEvent} and mirrors the uploaded chat/meeting document into
 * the project's Shared Library, so teammates who never open the chat still see meeting materials
 * there. The original file is not re-uploaded — the synced resource simply references the same
 * stored object the chat document already points to.
 *
 * Runs synchronously on the publisher's thread/transaction, same as {@code NotificationEventListener}:
 * a failure here is caught and logged so it never rolls back the document upload that triggered it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingLibrarySyncListener {

	private final ChatDocumentRepository chatDocumentRepository;
	private final ProjectRepository projectRepository;
	private final ProjectLibraryResourceRepository projectLibraryResourceRepository;

	@EventListener
	public void handleMeetingFileUploaded(MeetingFileUploadedEvent event) {
		try {
			syncToLibrary(event);
		} catch (RuntimeException exception) {
			log.warn(
				"Failed to sync meeting document {} into shared library for project {}",
				event.documentId(),
				event.projectId(),
				exception
			);
		}
	}

	private void syncToLibrary(MeetingFileUploadedEvent event) {
		if (projectLibraryResourceRepository.existsBySourceDocumentId(event.documentId())) {
			return;
		}

		ChatDocument document = chatDocumentRepository.findById(event.documentId()).orElse(null);
		if (document == null) {
			return;
		}

		Project project = projectRepository.getReferenceById(event.projectId());

		projectLibraryResourceRepository.save(ProjectLibraryResource.syncedFromMeetingDocument(
			project,
			document.getUploader(),
			document.getOriginalFileName(),
			document.getDescription(),
			document.getOriginalFileName(),
			document.getStoredFileName(),
			document.getFileUrl(),
			document.getFileSize(),
			document.getFileContentType(),
			document.getExtension(),
			document.getId()
		));
	}
}
