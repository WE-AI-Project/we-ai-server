package com.weai.server.domain.auth.repository;

import com.weai.server.domain.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	Optional<RefreshToken> findByUserId(Long userId);

	boolean existsByTokenHash(String tokenHash);
}
