package com.weai.server.domain.user.service;

import com.weai.server.domain.auth.request.SignUpRequest;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.domain.user.response.UserResponse;
import com.weai.server.global.dto.PageResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public PageResponse<UserResponse> findAll(Pageable pageable) {
		Pageable normalizedPageable = pageable.getSort().isSorted()
			? pageable
			: PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));

		return PageResponse.from(userRepository.findAll(normalizedPageable)
			.map(UserResponse::from)
		);
	}

	public UserResponse findById(Long userId) {
		return UserResponse.from(findUserEntityById(userId));
	}

	public UserResponse findByEmail(String email) {
		return UserResponse.from(getUserEntityByEmail(email));
	}

	public User getUserEntityByUsername(String username) {
		return userRepository.findByUsername(username)
			.orElseThrow(() -> new ApiException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"User with username '%s' could not be found.".formatted(username)
			));
	}

	public User getUserEntityByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> new ApiException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"User with email '%s' could not be found.".formatted(email)
			));
	}

	@Transactional
	public UserResponse registerUser(SignUpRequest request) {
		return registerUser(request, UserRole.USER);
	}

	@Transactional
	public void ensureBootstrapAdmin(String username, String rawPassword) {
		if (userRepository.existsByUsername(username)) {
			return;
		}

		User adminUser = User.create(
			username,
			passwordEncoder.encode(rawPassword),
			"Bootstrap Admin",
			username + "@local.we-ai",
			UserRole.ADMIN
		);

		userRepository.save(adminUser);
		log.info("Created bootstrap admin account '{}'.", username);
	}

	private UserResponse registerUser(SignUpRequest request, UserRole role) {
		validateDuplicateUser(request.username(), request.email());

		User createdUser = userRepository.save(User.create(
			request.username(),
			passwordEncoder.encode(request.password()),
			request.name(),
			request.email(),
			role
		));

		return UserResponse.from(createdUser);
	}

	private void validateDuplicateUser(String username, String email) {
		if (userRepository.existsByUsername(username)) {
			throw new ApiException(ErrorCode.CONFLICT, "Username '%s' is already in use.".formatted(username));
		}

		if (userRepository.existsByEmail(email)) {
			throw new ApiException(ErrorCode.CONFLICT, "Email '%s' is already in use.".formatted(email));
		}
	}

	private User findUserEntityById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ApiException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"User with id %d could not be found.".formatted(userId)
			));
	}
}
