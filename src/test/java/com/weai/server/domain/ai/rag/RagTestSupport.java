package com.weai.server.domain.ai.rag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

final class RagTestSupport {

	private RagTestSupport() {
	}

	static final class CountingEmbeddingModel implements EmbeddingModel {
		final AtomicInteger embeddedSegments = new AtomicInteger();

		@Override
		public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
			embeddedSegments.addAndGet(segments.size());
			return Response.from(segments.stream()
				.map(segment -> Embedding.from(new float[] {segment.text().length(), 1f, 0.5f}))
				.toList());
		}
	}

	static RagDocumentRepository inMemoryRepository() {
		Map<String, RagDocument> rows = new LinkedHashMap<>();
		RagDocumentRepository repository = mock(RagDocumentRepository.class);
		when(repository.findByProjectIdAndSource(anyLong(), anyString()))
			.thenAnswer(invocation -> Optional.ofNullable(rows.get(key(invocation.getArgument(0), invocation.getArgument(1)))));
		when(repository.save(any(RagDocument.class))).thenAnswer(invocation -> {
			RagDocument document = invocation.getArgument(0);
			rows.put(key(document.getProjectId(), document.getSource()), document);
			return document;
		});
		doAnswer(invocation -> {
			RagDocument document = invocation.getArgument(0);
			rows.remove(key(document.getProjectId(), document.getSource()));
			return null;
		}).when(repository).delete(any(RagDocument.class));
		when(repository.findAllByProjectIdAndOrigin(anyLong(), any())).thenAnswer(invocation -> rows.values().stream()
			.filter(document -> document.getProjectId().equals(invocation.getArgument(0))
				&& document.getOrigin() == invocation.getArgument(1))
			.toList());
		when(repository.findAllByProjectIdOrderBySourceAsc(anyLong())).thenAnswer(invocation -> rows.values().stream()
			.filter(document -> document.getProjectId().equals(invocation.getArgument(0)))
			.sorted((a, b) -> a.getSource().compareTo(b.getSource()))
			.toList());
		return repository;
	}

	private static String key(Long projectId, String source) {
		return projectId + "|" + source;
	}
}
