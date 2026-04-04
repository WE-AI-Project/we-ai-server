package com.weai.server.domain.auth.domain;

import com.weai.server.domain.user.domain.User;
import com.weai.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "refresh_tokens",
	indexes = {
		@Index(name = "idx_refresh_tokens_token", columnList = "token", unique = true),
		@Index(name = "idx_refresh_tokens_user_id", columnList = "user_id", unique = true)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "token", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(nullable = false)
	private Instant expiresAt;

	public static RefreshToken issue(User user, String tokenHash, Instant expiresAt) {
		return RefreshToken.builder()
			.user(user)
			.tokenHash(tokenHash)
			.expiresAt(expiresAt)
			.build();
	}

	public void rotate(String nextTokenHash, Instant nextExpiresAt) {
		this.tokenHash = nextTokenHash;
		this.expiresAt = nextExpiresAt;
	}

	public boolean isExpired(Instant now) {
		return !expiresAt.isAfter(now);
	}
}
