package com.weai.server.domain.auth.repository;

import com.weai.server.domain.auth.domain.VerificationCode;
import com.weai.server.domain.auth.domain.VerificationCodePurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

	Optional<VerificationCode> findTopByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
		String email,
		VerificationCodePurpose purpose
	);
}
