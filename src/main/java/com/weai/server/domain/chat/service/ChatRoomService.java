package com.weai.server.domain.chat.service;

import com.weai.server.domain.chat.domain.ChatMessage;
import com.weai.server.domain.chat.domain.ChatMessageType;
import com.weai.server.domain.chat.domain.ChatRoom;
import com.weai.server.domain.chat.domain.ChatRoomMember;
import com.weai.server.domain.chat.domain.ChatRoomMemberStatus;
import com.weai.server.domain.chat.domain.ChatRoomType;
import com.weai.server.domain.chat.repository.ChatMessageRepository;
import com.weai.server.domain.chat.repository.ChatRoomMemberRepository;
import com.weai.server.domain.chat.repository.ChatRoomRepository;
import com.weai.server.domain.chat.request.ChatMessageSendRequest;
import com.weai.server.domain.chat.response.ChatFileUploadResponse;
import com.weai.server.domain.chat.response.ChatMessageListResponse;
import com.weai.server.domain.chat.response.ChatMessageSendResponse;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final int DEFAULT_MESSAGE_SIZE = 30;
	private static final int MAX_MESSAGE_SIZE = 100;
	private static final int MAX_TEXT_MESSAGE_LENGTH = 2000;
	private static final int MAX_FILE_MESSAGE_LENGTH = 500;

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatFileStorageService chatFileStorageService;
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

	public ChatMessageListResponse getChatMessages(
		String userEmail,
		Long projectId,
		Long chatRoomId,
		Long beforeMessageId,
		Integer size,
		String keyword
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		ChatRoom chatRoom = validateChatRoomAccess(projectId, chatRoomId, user.getId());
		int resolvedSize = normalizeMessageSize(size);

		List<ChatMessage> fetchedMessages = chatMessageRepository.findMessagesBefore(
			chatRoom.getId(),
			beforeMessageId,
			normalizeKeyword(keyword),
			PageRequest.of(0, resolvedSize + 1)
		);
		boolean hasNext = fetchedMessages.size() > resolvedSize;
		List<ChatMessage> pageMessages = fetchedMessages;
		if (hasNext) {
			pageMessages = fetchedMessages.subList(0, resolvedSize);
		}

		List<ChatMessage> orderedMessages = new ArrayList<>(pageMessages);
		Collections.reverse(orderedMessages);
		Long nextCursor = hasNext && !orderedMessages.isEmpty() ? orderedMessages.get(0).getId() : null;

		return ChatMessageListResponse.from(
			projectId,
			chatRoom.getId(),
			resolvedSize,
			hasNext,
			nextCursor,
			orderedMessages,
			user.getId()
		);
	}

	@Transactional
	public ChatMessageSendResponse sendChatMessage(
		String userEmail,
		Long projectId,
		Long chatRoomId,
		ChatMessageSendRequest request
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		ChatRoom chatRoom = validateChatRoomAccess(projectId, chatRoomId, user.getId());
		String content = validateTextMessageContent(request == null ? null : request.content());
		ChatMessageType messageType = request == null || request.messageType() == null
			? ChatMessageType.TEXT
			: request.messageType();
		if (messageType != ChatMessageType.TEXT) {
			throw new ApiException(ErrorCode.INVALID_CHAT_MESSAGE_TYPE);
		}

		ChatMessage savedMessage = chatMessageRepository.save(ChatMessage.text(chatRoom, user, content));
		chatRoomRepository.touchUpdatedAt(chatRoom.getId(), LocalDateTime.now());
		return ChatMessageSendResponse.from(savedMessage);
	}

	@Transactional
	public ChatFileUploadResponse uploadChatFile(
		String userEmail,
		Long projectId,
		Long chatRoomId,
		MultipartFile file,
		String content
	) {
		User user = userService.getUserEntityByEmail(userEmail);
		ChatRoom chatRoom = validateChatRoomAccess(projectId, chatRoomId, user.getId());
		String normalizedContent = validateFileMessageContent(content);
		ChatFileStorageService.StoredChatFile storedFile = chatFileStorageService.store(projectId, chatRoom.getId(), file);

		ChatMessage savedMessage = chatMessageRepository.save(ChatMessage.file(
			chatRoom,
			user,
			normalizedContent,
			storedFile.messageType(),
			storedFile.fileUrl(),
			storedFile.originalFileName(),
			storedFile.storedFileName(),
			storedFile.fileSize(),
			storedFile.fileContentType()
		));
		chatRoomRepository.touchUpdatedAt(chatRoom.getId(), LocalDateTime.now());
		return ChatFileUploadResponse.from(savedMessage);
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

	private ChatRoom validateChatRoomAccess(Long projectId, Long chatRoomId, Long userId) {
		projectService.validateProjectAccess(projectId, userId);
		ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
			.orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

		if (!chatRoom.getProject().getId().equals(projectId)) {
			throw new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND);
		}
		if (!chatRoom.isActive()) {
			throw new ApiException(ErrorCode.CHAT_ROOM_NOT_ACTIVE);
		}
		if (chatRoom.isPrivate()
			&& chatRoomMemberRepository
			.findByChatRoom_IdAndUser_IdAndStatus(chatRoomId, userId, ChatRoomMemberStatus.ACTIVE)
			.isEmpty()) {
			throw new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
		}
		return chatRoom;
	}

	private String validateTextMessageContent(String content) {
		String normalizedContent = trimToNull(content);
		if (normalizedContent == null) {
			throw new ApiException(ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);
		}
		if (normalizedContent.length() > MAX_TEXT_MESSAGE_LENGTH) {
			throw new ApiException(ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG);
		}
		return normalizedContent;
	}

	private String validateFileMessageContent(String content) {
		String normalizedContent = trimToNull(content);
		if (normalizedContent != null && normalizedContent.length() > MAX_FILE_MESSAGE_LENGTH) {
			throw new ApiException(ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG);
		}
		return normalizedContent;
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

	private int normalizeMessageSize(Integer size) {
		int resolvedSize = size == null ? DEFAULT_MESSAGE_SIZE : size;
		if (resolvedSize <= 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT, "size must be greater than 0.");
		}
		return Math.min(resolvedSize, MAX_MESSAGE_SIZE);
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
