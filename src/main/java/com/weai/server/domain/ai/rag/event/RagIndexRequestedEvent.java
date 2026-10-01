package com.weai.server.domain.ai.rag.event;

import com.weai.server.domain.ai.rag.RagDocumentOrigin;

/** Asks for one text document to be (re)indexed into a project's RAG store after the current transaction commits. */
public record RagIndexRequestedEvent(Long projectId, String source, String text, RagDocumentOrigin origin) {
}
