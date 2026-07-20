package com.weai.server.domain.chat.service;

import com.weai.server.domain.chat.domain.ChatMessage;
import com.weai.server.domain.chat.domain.ChatRoom;
import com.weai.server.domain.chat.domain.ChatRoomMember;
import com.weai.server.domain.chat.domain.ChatRoomMemberStatus;
import com.weai.server.domain.chat.domain.ChatRoomType;
import com.weai.server.domain.chat.repository.ChatMessageRepository;
import com.weai.server.domain.chat.repository.ChatRoomMemberRepository;
import com.weai.server.domain.chat.repository.ChatRoomRepository;
import com.weai.server.domain.chat.response.ChatRoomListResponse;
import com.weai.server.domain.chat.response.ChatRoomListResponse.ChatRoomResponse;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectService projectService;
	private final UserService userService;

	public ChatRoomListResponse getChatRooms(
		String userEmail,
		Long projectId,
		String type,
		String department,
		String keyword,
		Integer page,
		Integer size
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		projectService.validateProjectAccess(projectId, user.getId());

		ChatRoomType chatRoomType = parseChatRoomType(type);
		ProjectDepartment projectDepartment = parseDepartment(department);
		Pageable pageable = normalizePageable(page, size);

		Page<ChatRoom> chatRoomPage = chatRoomRepository.findAccessibleRooms(
			projectId,
			user.getId(),
			chatRoomType,
			projectDepartment,
			normalizeKeyword(keyword),
			pageable
		);

		Page<ChatRoomResponse> responsePage = new PageImpl<>(
			chatRoomPage.getContent().stream()
				.map(chatRoom -> toChatRoomResponse(chatRoom, user.getId()))
				.toList(),
			pageable,
			chatRoomPage.getTotalElements()
		);
		return ChatRoomListResponse.from(projectId, responsePage);
	}

	private ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, Long userId) {
		ChatMessage lastMessage = chatMessageRepository
			.findTopByChatRoom_IdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(chatRoom.getId())
			.orElse(null);
		long memberCount = countMembers(chatRoom);
		long unreadCount = countUnreadMessages(chatRoom.getId(), userId);
		return ChatRoomResponse.from(chatRoom, memberCount, unreadCount, lastMessage);
	}

	private long countMembers(ChatRoom chatRoom) {
		if (!chatRoom.isPrivate()) {
			return projectMemberRepository.countByProject_IdAndStatus(
				chatRoom.getProject().getId(),
				ProjectMemberStatus.ACTIVE
			);
		}
		return chatRoomMemberRepository.countByChatRoom_IdAndStatus(chatRoom.getId(), ChatRoomMemberStatus.ACTIVE);
	}

	private long countUnreadMessages(Long chatRoomId, Long userId) {
		ChatRoomMember chatRoomMember = chatRoomMemberRepository
			.findByChatRoom_IdAndUser_IdAndStatus(chatRoomId, userId, ChatRoomMemberStatus.ACTIVE)
			.orElse(null);
		if (chatRoomMember == null) {
			return 0;
		}
		if (chatRoomMember.getLastReadMessage() == null) {
			return chatMessageRepository.countByChatRoom_IdAndSender_IdNotAndDeletedAtIsNull(chatRoomId, userId);
		}
		return chatMessageRepository.countByChatRoom_IdAndIdGreaterThanAndSender_IdNotAndDeletedAtIsNull(
			chatRoomId,
			chatRoomMember.getLastReadMessage().getId(),
			userId
		);
	}

	private ChatRoomType parseChatRoomType(String type) {
		String normalizedType = trimToNull(type);
		if (normalizedType == null) {
			return null;
		}

		try {
			return ChatRoomType.valueOf(normalizedType.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_CHAT_ROOM_TYPE);
		}
	}

	private ProjectDepartment parseDepartment(String department) {
		String normalizedDepartment = trimToNull(department);
		if (normalizedDepartment == null) {
			return null;
		}

		try {
			return ProjectDepartment.valueOf(normalizedDepartment.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.INVALID_DEPARTMENT);
		}
	}

	private String normalizeKeyword(String keyword) {
		return trimToNull(keyword);
	}

	private Pageable normalizePageable(Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		if (resolvedPage < 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "page must be greater than or equal to 0.");
		}
		if (resolvedSize <= 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "size must be greater than 0.");
		}
		return PageRequest.of(resolvedPage, Math.min(resolvedSize, MAX_SIZE));
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
