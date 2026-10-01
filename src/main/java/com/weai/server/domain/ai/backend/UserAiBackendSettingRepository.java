package com.weai.server.domain.ai.backend;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAiBackendSettingRepository extends JpaRepository<UserAiBackendSetting, Long> {
	Optional<UserAiBackendSetting> findByUser_Id(Long userId);
}
