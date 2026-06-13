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
		if (isEmpty()) {
			return "No project-isolated RAG documents were retrieved for Project ID: " + projectId + ".";
		}

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < chunks.size(); i++) {
			builder
				.append("\n\n[Project ")
				.append(projectId)
				.append(" / RAG Document ")
				.append(i + 1)
				.append("]\n")
				.append(chunks.get(i));
		}
		return builder.toString().trim();
	}
}
