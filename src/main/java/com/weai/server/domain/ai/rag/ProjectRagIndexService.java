package com.weai.server.domain.ai.rag;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Indexes (and re-indexes / deletes) project documents in the shared RAG vector store.
 *
 * Indexing is an upsert per (projectId, source): previously indexed chunks for that pair are removed
 * first. The {@link RagDocument} registry records a hash of what was embedded, so re-indexing
 * identical content (repeated saves, re-uploaded workspaces) is skipped instead of re-embedded.
 */
@Slf4j
@Service
public class ProjectRagIndexService {

	public static final int MAX_SOURCE_LENGTH = 500;

	private static final int CHUNK_SIZE = 1_200;
	private static final int CHUNK_OVERLAP = 180;
	private static final int LOCK_STRIPES = 64;

	private final EmbeddingStore<TextSegment> embeddingStore;
	private final EmbeddingModel embeddingModel;
	private final RagDocumentRepository ragDocumentRepository;
	private final String embeddingModelName;
	private final Object[] locks = new Object[LOCK_STRIPES];

	public ProjectRagIndexService(
		@Qualifier("oracleChromaEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore,
		@Qualifier("oracleEmbeddingModel") EmbeddingModel embeddingModel,
		RagDocumentRepository ragDocumentRepository,
		@Value("${ai.chat.embedding-model-name:${AI_CHAT_EMBEDDING_MODEL_NAME:nomic-embed-text}}") String embeddingModelName
	) {
		this.embeddingStore = embeddingStore;
		this.embeddingModel = embeddingModel;
		this.ragDocumentRepository = ragDocumentRepository;
		this.embeddingModelName = embeddingModelName;
		for (int i = 0; i < LOCK_STRIPES; i++) {
			locks[i] = new Object();
		}
	}

	public RagDocumentIndexResponse index(Long projectId, String source, String text) {
		return index(projectId, source, text, RagDocumentOrigin.MANUAL);
	}

	public RagDocumentIndexResponse index(Long projectId, String source, String text, RagDocumentOrigin origin) {
		String normalizedSource = validateSource(projectId, source);
		if (!StringUtils.hasText(text)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "text is required.");
		}
		if (RagSourcePolicy.isSecretFile(normalizedSource)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "Secret files (.env, keys, credentials) cannot be indexed.");
		}

		String content = RagSourcePolicy.redactSecrets(normalizedSource, text.trim());
		String contentHash = sha256(embeddingModelName + "\n" + content);

		synchronized (lockFor(projectId, normalizedSource)) {
			Optional<RagDocument> existing = ragDocumentRepository.findByProjectIdAndSource(projectId, normalizedSource);
			if (existing.isPresent() && existing.get().isUpToDate(contentHash, embeddingModelName)) {
				return new RagDocumentIndexResponse(projectId, normalizedSource, existing.get().getChunkCount(), List.of(), true);
			}

			removeExisting(projectId, normalizedSource);
			List<TextSegment> segments = toSegments(projectId, normalizedSource, origin, content);
			List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
			List<String> ids = embeddingStore.addAll(embeddings, segments);

			RagDocument document = existing.orElseGet(() -> RagDocument.create(projectId, normalizedSource));
			document.markIndexed(origin, segments.size(), contentHash, embeddingModelName);
			ragDocumentRepository.save(document);

			return new RagDocumentIndexResponse(projectId, normalizedSource, segments.size(), List.copyOf(ids), false);
		}
	}

	/** Removes every previously indexed chunk for one document without re-indexing it. */
	public void delete(Long projectId, String source) {
		String normalizedSource = validateSource(projectId, source);
		synchronized (lockFor(projectId, normalizedSource)) {
			removeExisting(projectId, normalizedSource);
			ragDocumentRepository.findByProjectIdAndSource(projectId, normalizedSource).ifPresent(ragDocumentRepository::delete);
		}
	}

	public List<RagDocument> list(Long projectId) {
		return ragDocumentRepository.findAllByProjectIdOrderBySourceAsc(projectId);
	}

	public List<RagDocument> listByOrigin(Long projectId, RagDocumentOrigin origin) {
		return ragDocumentRepository.findAllByProjectIdAndOrigin(projectId, origin);
	}

	private String validateSource(Long projectId, String source) {
		if (projectId == null) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "projectId is required.");
		}
		if (!StringUtils.hasText(source)) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "source is required.");
		}
		String normalized = source.trim().replace('\\', '/');
		if (normalized.length() > MAX_SOURCE_LENGTH) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "source must be " + MAX_SOURCE_LENGTH + " characters or fewer.");
		}
		return normalized;
	}

	private Object lockFor(Long projectId, String source) {
		return locks[Math.floorMod((projectId + ":" + source).hashCode(), LOCK_STRIPES)];
	}

	private void removeExisting(Long projectId, String source) {
		Filter filter = metadataKey("projectId").isEqualTo(projectId).and(metadataKey("source").isEqualTo(source));
		try {
			embeddingStore.removeAll(filter);
		} catch (RuntimeException exception) {
			log.warn("Failed to remove previously indexed RAG chunks for projectId={}, source={}", projectId, source, exception);
		}
	}

	private List<TextSegment> toSegments(Long projectId, String source, RagDocumentOrigin origin, String text) {
		List<String> chunks = chunk(text);
		List<TextSegment> segments = new ArrayList<>();
		String indexedAt = Instant.now().toString();
		for (int i = 0; i < chunks.size(); i++) {
			Metadata metadata = new Metadata()
				.put("projectId", projectId)
				.put("source", source)
				.put("origin", origin.name())
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

	private static String sha256(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
