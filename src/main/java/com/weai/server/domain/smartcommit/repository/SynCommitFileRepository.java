package com.weai.server.domain.smartcommit.repository;

import com.weai.server.domain.smartcommit.domain.SynCommitFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SynCommitFileRepository extends JpaRepository<SynCommitFile, Long> {

	List<SynCommitFile> findBySynCommit_IdOrderByFilePathAsc(Long synCommitId);
}
