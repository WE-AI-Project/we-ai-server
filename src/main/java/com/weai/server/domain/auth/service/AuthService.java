package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.domain.RefreshToken;
import com.weai.server.domain.auth.dto.google.GoogleUserResponse;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.auth.dto.naver.NaverUserResponse;
import com.weai.server.domain.auth.repository.RefreshTokenRepository;
import com.weai.server.domain.auth.request.LoginRequest;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	private final NaverOAuthService naverOAuthService;
	private final GoogleOAuthService googleOAuthService;

	@Transactional
	public TokenResponse login(LoginRequest request) {
		// 1. 유저 존재 여부 확인 (record이므로 request.email() 호출)
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "가입되지 않은 이메일이거나 사용자를 찾을 수 없습니다."));

		// 2. 비밀번호 일치 여부 검증 (record이므로 request.password() 호출)
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new ApiException(ErrorCode.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
		}

		// 3. JWT Access Token 발급 (User 엔티티 전체를 넘김)
		String accessToken = jwtTokenProvider.createAccessToken(user);
		long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

		// 4. Refresh Token 생성 및 저장 (고유 UUID 생성 후 저장)
		String refreshTokenString = UUID.randomUUID().toString();
		long refreshTokenExpiresIn = 604800; // 예: 7일(초 단위)
		Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpiresIn);

		// RefreshToken 엔티티의 정적 팩토리 메서드 활용
		RefreshToken refreshToken = RefreshToken.issue(user, refreshTokenString, expiresAt);
		refreshTokenRepository.save(refreshToken);

		// 5. TokenResponse 반환 (record 생성자 파라미터 순서에 맞춤)
		return new TokenResponse(
				"Bearer",
				accessToken,
				accessTokenExpiresIn,
				refreshTokenString,
				refreshTokenExpiresIn,
				user.getUsername(),
				user.getEmail(),
				user.getRole()
		);
	}

	@Transactional
	public TokenResponse naverLogin(String code, String state) {
		// 1. 네이버 서버로부터 Access Token 받아오기
		String naverAccessToken = naverOAuthService.getAccessToken(code, state);

		// 2. Access Token으로 유저 정보 받아오기
		NaverUserResponse naverUserInfo = naverOAuthService.getUserInfo(naverAccessToken);
		NaverUserResponse.Response profile = naverUserInfo.getResponse();

		// 3. DB에 유저가 있는지 이메일로 확인하고, 없으면 회원가입(저장) 처리
		User user = userRepository.findByEmail(profile.getEmail())
				.orElseGet(() -> saveNaverUser(profile));

		// 4. 기존 login 로직과 동일하게 JWT 토큰(Access/Refresh) 발급 및 저장
		String accessToken = jwtTokenProvider.createAccessToken(user);
		long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

		String refreshTokenString = UUID.randomUUID().toString();
		long refreshTokenExpiresIn = 604800; // 7일(초 단위)
		Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpiresIn);

		RefreshToken refreshToken = RefreshToken.issue(user, refreshTokenString, expiresAt);
		refreshTokenRepository.findByUserId(user.getId())
				.ifPresentOrElse(
						// 4-1. 기존 토큰이 존재하면 새로운 값으로 업데이트 (rotate)
						existingToken -> existingToken.rotate(refreshTokenString, expiresAt),
						// 4-2. 기존 토큰이 없으면(첫 로그인) 새로 생성하여 저장
						() -> refreshTokenRepository.save(RefreshToken.issue(user, refreshTokenString, expiresAt))
				);

		// 5. 응답 반환
		return new TokenResponse(
				"Bearer",
				accessToken,
				accessTokenExpiresIn,
				refreshTokenString,
				refreshTokenExpiresIn,
				user.getUsername(),
				user.getEmail(),
				user.getRole()
		);
	}

	private User saveNaverUser(NaverUserResponse.Response profile) {
		// 1. 네이버에서 넘겨준 실제 이름 (없을 경우 임시 이름 부여)
		String realName = profile.getName() != null ? profile.getName() : "네이버유저";

		// 2. 닉네임이 있으면 닉네임 사용, 없으면 실제 이름 사용
		String username = profile.getNickname() != null ? profile.getNickname() : realName;

		// 3. 만약 둘 다 없거나 중복 방지가 필요하다면 임의 문자열 추가
		if (username.equals("네이버유저")) {
			username = "네이버_" + UUID.randomUUID().toString().substring(0, 8);
		}

		User newUser = User.builder()
				.email(profile.getEmail())
				.name(realName)
				.username(username)
				.password(passwordEncoder.encode(UUID.randomUUID().toString())) // 소셜 로그인은 임의 암호화
				.role(UserRole.USER)
				.build();

		return userRepository.save(newUser);
	}

	@Transactional
	public TokenResponse googleLogin(String code) {
		// 1. 구글 서버로부터 Access Token 받아오기
		String googleAccessToken = googleOAuthService.getAccessToken(code);

		// 2. Access Token으로 유저 정보 받아오기
		GoogleUserResponse profile = googleOAuthService.getUserInfo(googleAccessToken);

		// 3. DB에 유저 확인 및 저장 (이메일 기준)
		User user = userRepository.findByEmail(profile.getEmail())
				.orElseGet(() -> saveGoogleUser(profile));

		// 4. JWT 토큰 발급
		String accessToken = jwtTokenProvider.createAccessToken(user);
		long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

		String refreshTokenString = UUID.randomUUID().toString();
		long refreshTokenExpiresIn = 604800; // 7일
		Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpiresIn);

		// 5. Refresh Token 갱신 또는 생성
		refreshTokenRepository.findByUserId(user.getId())
				.ifPresentOrElse(
						existingToken -> existingToken.rotate(refreshTokenString, expiresAt),
						() -> refreshTokenRepository.save(RefreshToken.issue(user, refreshTokenString, expiresAt))
				);

		return new TokenResponse(
				"Bearer",
				accessToken,
				accessTokenExpiresIn,
				refreshTokenString,
				refreshTokenExpiresIn,
				user.getUsername(),
				user.getEmail(),
				user.getRole()
		);
	}

	private User saveGoogleUser(GoogleUserResponse profile) {
		String realName = profile.getName() != null ? profile.getName() : "구글유저";

		// 구글은 닉네임을 따로 주지 않으므로 이름이나 이메일 앞부분을 username으로 사용
		String username = profile.getEmail().split("@")[0];

		User newUser = User.builder()
				.email(profile.getEmail())
				.name(realName)
				.username(username)
				.password(passwordEncoder.encode(UUID.randomUUID().toString()))
				.role(UserRole.USER)
				.build();

		return userRepository.save(newUser);
	}

}