package com.weai.server.domain.chat.repository;

import com.weai.server.domain.chat.domain.ChatDocument;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatDocumentRepository extends JpaRepository<ChatDocument, Long> {

	Optional<ChatDocument> findByIdAndProject_IdAndDeletedAtIsNull(Long id, Long projectId);
}
