package com.weai.server.domain.project.repository;

import com.weai.server.domain.project.domain.DailyStandupDismissal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyStandupDismissalRepository extends JpaRepository<DailyStandupDismissal, Long> {

	boolean existsByProject_IdAndUser_IdAndDismissDate(Long projectId, Long userId, LocalDate dismissDate);

	Optional<DailyStandupDismissal> findByProject_IdAndUser_IdAndDismissDate(
		Long projectId,
		Long userId,
		LocalDate dismissDate
	);
}
