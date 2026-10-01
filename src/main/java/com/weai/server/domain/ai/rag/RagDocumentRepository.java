package com.weai.server.domain.ai.rag;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {

	Optional<RagDocument> findByProjectIdAndSource(Long projectId, String source);

	List<RagDocument> findAllByProjectIdOrderBySourceAsc(Long projectId);

	List<RagDocument> findAllByProjectIdAndOrigin(Long projectId, RagDocumentOrigin origin);
}
