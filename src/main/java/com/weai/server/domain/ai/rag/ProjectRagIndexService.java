package com.weai.server.domain.ai.rag;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProjectRagIndexService {

	private static final int CHUNK_SIZE = 1_200;
	private static final int CHUNK_OVERLAP = 180;

	private final EmbeddingStore<TextSegment> embeddingStore;
	private final EmbeddingModel embeddingModel;

	public ProjectRagIndexService(
		@Qualifier("oracleChromaEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore,
		@Qualifier("oracleEmbeddingModel") EmbeddingModel embeddingModel
	) {
		this.embeddingStore = embeddingStore;
		this.embeddingModel = embeddingModel;
	}

	public RagDocumentIndexResponse index(Long projectId, String source, String text) {
		if (projectId == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "projectId is required.");
		}
		if (!StringUtils.hasText(source)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "source is required.");
		}
		if (!StringUtils.hasText(text)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "text is required.");
		}

		List<TextSegment> segments = toSegments(projectId, source.trim(), text.trim());
		List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
		List<String> ids = embeddingStore.addAll(embeddings, segments);

		return new RagDocumentIndexResponse(projectId, source.trim(), segments.size(), List.copyOf(ids));
	}

	private List<TextSegment> toSegments(Long projectId, String source, String text) {
		List<String> chunks = chunk(text);
		List<TextSegment> segments = new ArrayList<>();
		String indexedAt = Instant.now().toString();
		for (int i = 0; i < chunks.size(); i++) {
			Metadata metadata = new Metadata()
				.put("projectId", projectId)
				.put("source", source)
				.put("chunkIndex", i)
				.put("indexedAt", indexedAt);
			segments.add(TextSegment.from(chunks.get(i), metadata));
		}
		return segments;
	}

	private List<String> chunk(String text) {
		List<String> chunks = new ArrayList<>();
		int start = 0;
		while (start < text.length()) {
			int end = Math.min(start + CHUNK_SIZE, text.length());
			int splitAt = findSplitPoint(text, start, end);
			chunks.add(text.substring(start, splitAt).trim());
			if (splitAt >= text.length()) {
				break;
			}
			start = Math.max(splitAt - CHUNK_OVERLAP, start + 1);
		}
		return chunks.stream()
			.filter(StringUtils::hasText)
			.toList();
	}

	private int findSplitPoint(String text, int start, int end) {
		if (end == text.length()) {
			return end;
		}
		int paragraphBreak = text.lastIndexOf("\n\n", end);
		if (paragraphBreak > start + CHUNK_SIZE / 2) {
			return paragraphBreak;
		}
		int lineBreak = text.lastIndexOf('\n', end);
		if (lineBreak > start + CHUNK_SIZE / 2) {
			return lineBreak;
		}
		int space = text.lastIndexOf(' ', end);
		if (space > start + CHUNK_SIZE / 2) {
			return space;
		}
		return end;
	}
}
