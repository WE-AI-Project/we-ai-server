package com.weai.server.domain.auth.domain;

import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "verification_codes",
	indexes = {
		@Index(name = "idx_verification_codes_email_purpose_used_created", columnList = "email, purpose, used_at, created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class VerificationCode extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private VerificationCodePurpose purpose;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VerificationDeliveryChannel deliveryChannel;

	@Column(nullable = false, length = 255)
	private String deliveryTarget;

	@Column(nullable = false, length = 64)
	private String codeHash;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	@Column
	private LocalDateTime usedAt;

	public static VerificationCode issue(
		String email,
		VerificationCodePurpose purpose,
		VerificationDeliveryChannel deliveryChannel,
		String deliveryTarget,
		String codeHash,
		LocalDateTime expiresAt
	) {
		return VerificationCode.builder()
			.email(email)
			.purpose(purpose)
			.deliveryChannel(deliveryChannel)
			.deliveryTarget(deliveryTarget)
			.codeHash(codeHash)
			.expiresAt(expiresAt)
			.build();
	}

	public boolean isExpired(LocalDateTime now) {
		return !expiresAt.isAfter(now);
	}

	public boolean matches(String hashedCode) {
		return codeHash.equals(hashedCode);
	}

	public void markUsed(LocalDateTime now) {
		this.usedAt = now;
	}
}
