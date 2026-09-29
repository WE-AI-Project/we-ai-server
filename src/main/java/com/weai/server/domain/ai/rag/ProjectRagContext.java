package com.weai.server.domain.ai.rag;

import java.util.List;

public record ProjectRagContext(
	Long projectId,
	List<String> chunks
) {

	public boolean isEmpty() {
		return chunks == null || chunks.isEmpty();
	}

	public String formatted() {
		return RagContextSanitizer.wrap(projectId, chunks);
	}
}
