package com.weai.server.domain.chat.service;

import com.weai.server.domain.chat.domain.ChatRoom;
import com.weai.server.domain.chat.domain.ChatRoomMember;
import com.weai.server.domain.chat.domain.ChatRoomMemberStatus;
import com.weai.server.domain.chat.domain.ChatRoomStatus;
import com.weai.server.domain.chat.repository.ChatRoomMemberRepository;
import com.weai.server.domain.chat.repository.ChatRoomRepository;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectMemberStatus;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomProjectLifecycleService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ProjectMemberRepository projectMemberRepository;

	@Transactional
	public ChatRoom createDefaultChatRoom(Project project) {
		if (chatRoomRepository.existsByProject_IdAndIsDefaultTrueAndStatusAndDeletedAtIsNull(
			project.getId(),
			ChatRoomStatus.ACTIVE
		)) {
			throw new ApiException(ErrorCode.DEFAULT_CHAT_ROOM_ALREADY_EXISTS);
		}

		try {
			ChatRoom chatRoom = chatRoomRepository.saveAndFlush(ChatRoom.createDefault(project, project.getCreatedBy()));
			List<ChatRoomMember> roomMembers = projectMemberRepository
				.findByProjectIdAndStatusWithUser(project.getId(), ProjectMemberStatus.ACTIVE)
				.stream()
				.map(projectMember -> ChatRoomMember.active(chatRoom, projectMember.getUser()))
				.toList();
			chatRoomMemberRepository.saveAll(roomMembers);
			return chatRoom;
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.DEFAULT_CHAT_ROOM_ALREADY_EXISTS);
		}
	}

	@Transactional
	public void addToDefaultChatRoom(Project project, User user) {
		ChatRoom defaultRoom = chatRoomRepository
			.findByProject_IdAndIsDefaultTrueAndStatusAndDeletedAtIsNull(project.getId(), ChatRoomStatus.ACTIVE)
			.orElseGet(() -> createDefaultChatRoom(project));

		ChatRoomMember roomMember = chatRoomMemberRepository
			.findByChatRoom_IdAndUser_Id(defaultRoom.getId(), user.getId())
			.orElse(null);
		if (roomMember == null) {
			chatRoomMemberRepository.save(ChatRoomMember.active(defaultRoom, user));
		} else if (roomMember.getStatus() != ChatRoomMemberStatus.ACTIVE) {
			roomMember.reactivate();
		}
	}

	@Transactional
	public void leaveProjectChatRooms(Long projectId, Long userId) {
		chatRoomMemberRepository.findActiveByProjectIdAndUserId(projectId, userId)
			.forEach(ChatRoomMember::leave);
	}

	@Transactional
	public void kickFromProjectChatRooms(Long projectId, Long userId) {
		chatRoomMemberRepository.findActiveByProjectIdAndUserId(projectId, userId)
			.forEach(ChatRoomMember::kick);
	}
}
