package com.weai.server.domain.user.service;

import com.weai.server.domain.user.dto.CreateUserRequest;
import com.weai.server.domain.user.dto.UserResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	private final AtomicLong sequence = new AtomicLong(1L);
	private final Map<Long, UserResponse> users = new ConcurrentHashMap<>();

	public UserService() {
		users.put(1L, new UserResponse(1L, "홍길동", "gildong@example.com"));
	}

	public List<UserResponse> findAll() {
		return users.values().stream()
			.sorted(Comparator.comparing(UserResponse::id))
			.toList();
	}

	public UserResponse findById(Long userId) {
		UserResponse user = users.get(userId);
		if (user == null) {
			throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId));
		}
		return user;
	}

	public UserResponse create(CreateUserRequest request) {
		long userId = sequence.incrementAndGet();
		UserResponse createdUser = new UserResponse(userId, request.name(), request.email());
		users.put(userId, createdUser);
		return createdUser;
	}
}
