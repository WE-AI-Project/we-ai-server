package com.weai.server.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.weai.server.domain.chat.request.ChatRoomCreateRequest;
import com.weai.server.domain.chat.response.ChatFileUploadResponse;
import com.weai.server.domain.chat.response.ChatMessageListResponse;
import com.weai.server.domain.chat.response.ChatMessageSendResponse;
import com.weai.server.domain.chat.response.ChatRoomListResponse;
import com.weai.server.domain.chat.response.ChatRoomListResponse.ChatRoomResponse;
import com.weai.server.domain.chat.response.ChatRoomCreateResponse;
import com.weai.server.domain.chat.response.ProjectDepartmentListResponse;
import com.weai.server.domain.project.domain.Project;
import com.weai.server.domain.project.domain.ProjectDepartment;
import com.weai.server.domain.project.domain.ProjectMember;
import com.weai.server.domain.project.repository.ProjectMemberRepository;
import com.weai.server.domain.project.repository.ProjectRepository;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatRoomServiceTest {

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private ChatRoomProjectLifecycleService chatRoomProjectLifecycleService;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ChatRoomMemberRepository chatRoomMemberRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void getChatRoomsReturnsEmptyArrayWhenProjectHasNoRooms() {
		TestFixture fixture = createFixture();

		ChatRoomListResponse response = chatRoomService.getChatRooms(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			null,
			null,
			null,
			0,
			20
		);

		assertThat(response.projectId()).isEqualTo(fixture.project().getId());
		assertThat(response.totalCount()).isZero();
		assertThat(response.chatRooms()).isEmpty();
	}

	@Test
	void getChatRoomsReturnsLastMessageAndUnreadCount() {
		TestFixture fixture = createFixture();
		ChatRoom generalRoom = saveRoom(
			fixture.project(),
			fixture.leader(),
			"전체 채팅",
			ChatRoomType.GENERAL,
			null,
			false
		);
		ChatRoom emptyRoom = saveRoom(
			fixture.project(),
			fixture.leader(),
			"Backend",
			ChatRoomType.DEPARTMENT,
			ProjectDepartment.BACKEND,
			false
		);
		ChatMessage firstMessage = chatMessageRepository.save(ChatMessage.text(
			generalRoom,
			fixture.member(),
			"첫 번째 메시지"
		));
		ChatMessage selfMessage = chatMessageRepository.save(ChatMessage.text(
			generalRoom,
			fixture.leader(),
			"내가 보낸 메시지"
		));
		ChatMessage lastMessage = chatMessageRepository.save(ChatMessage.text(
			generalRoom,
			fixture.member(),
			"오늘 일정 API 작업 완료했습니다."
		));
		chatRoomMemberRepository.save(ChatRoomMember.active(generalRoom, fixture.leader(), firstMessage));
		chatMessageRepository.flush();

		ChatRoomListResponse response = chatRoomService.getChatRooms(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			null,
			null,
			null,
			0,
			20
		);

		assertThat(response.totalCount()).isEqualTo(2);
		ChatRoomResponse generalResponse = findRoom(response, generalRoom.getId());
		ChatRoomResponse emptyResponse = findRoom(response, emptyRoom.getId());
		assertThat(generalResponse.lastMessage()).isNotNull();
		assertThat(generalResponse.lastMessage().messageId()).isEqualTo(lastMessage.getId());
		assertThat(generalResponse.lastMessage().senderId()).isEqualTo(fixture.member().getId());
		assertThat(generalResponse.memberCount()).isEqualTo(2);
		assertThat(generalResponse.unreadCount()).isEqualTo(1);
		assertThat(emptyResponse.lastMessage()).isNull();
		assertThat(emptyResponse.unreadCount()).isZero();
		assertThat(response.chatRooms()).extracting(ChatRoomResponse::chatRoomId)
			.contains(generalRoom.getId(), emptyRoom.getId());
		assertThat(selfMessage.getId()).isNotNull();
	}

	@Test
	void getChatRoomsFiltersByTypeDepartmentAndKeyword() {
		TestFixture fixture = createFixture();
		ChatRoom generalRoom = saveRoom(fixture.project(), fixture.leader(), "전체 채팅", ChatRoomType.GENERAL, null, false);
		ChatRoom backendRoom = saveRoom(
			fixture.project(),
			fixture.leader(),
			"Backend",
			ChatRoomType.DEPARTMENT,
			ProjectDepartment.BACKEND,
			false
		);
		saveRoom(fixture.project(), fixture.leader(), "Frontend", ChatRoomType.DEPARTMENT, ProjectDepartment.FRONTEND, false);

		ChatRoomListResponse typeResponse = chatRoomService.getChatRooms(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			"GENERAL",
			null,
			null,
			0,
			20
		);
		ChatRoomListResponse departmentResponse = chatRoomService.getChatRooms(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			null,
			"BACKEND",
			null,
			0,
			20
		);
		ChatRoomListResponse keywordResponse = chatRoomService.getChatRooms(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			null,
			null,
			"back",
			0,
			20
		);

		assertThat(typeResponse.chatRooms()).extracting(ChatRoomResponse::chatRoomId).containsExactly(generalRoom.getId());
		assertThat(departmentResponse.chatRooms()).extracting(ChatRoomResponse::chatRoomId).containsExactly(backendRoom.getId());
		assertThat(keywordResponse.chatRooms()).extracting(ChatRoomResponse::chatRoomId).containsExactly(backendRoom.getId());
	}

	@Test
	void getChatRoomsReturnsPrivateRoomOnlyForRoomMember() {
		TestFixture fixture = createFixture();
		ChatRoom privateRoom = saveRoom(
			fixture.project(),
			fixture.leader(),
			"비공개 채팅",
			ChatRoomType.DIRECT,
			null,
			true
		);
		chatRoomMemberRepository.save(ChatRoomMember.active(privateRoom, fixture.leader()));

		ChatRoomListResponse leaderResponse = chatRoomService.getChatRooms(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			null,
			null,
			null,
			0,
			20
		);
		ChatRoomListResponse memberResponse = chatRoomService.getChatRooms(
			fixture.member().getEmail(),
			fixture.project().getId(),
			null,
			null,
			null,
			0,
			20
		);

		assertThat(leaderResponse.chatRooms()).extracting(ChatRoomResponse::chatRoomId).containsExactly(privateRoom.getId());
		assertThat(memberResponse.chatRooms()).isEmpty();
	}

	@Test
	void getChatRoomsRejectsNonProjectMember() {
		TestFixture fixture = createFixture();

		assertThatThrownBy(() -> chatRoomService.getChatRooms(
			fixture.outsider().getEmail(),
			fixture.project().getId(),
			null,
			null,
			null,
			0,
			20
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
	}

	@Test
	void getChatRoomsRejectsMissingProject() {
		TestFixture fixture = createFixture();

		assertThatThrownBy(() -> chatRoomService.getChatRooms(
			fixture.leader().getEmail(),
			999_999L,
			null,
			null,
			null,
			0,
			20
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
	}

	@Test
	void getChatRoomsRejectsInvalidFilters() {
		TestFixture fixture = createFixture();

		assertThatThrownBy(() -> chatRoomService.getChatRooms(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			"UNKNOWN",
			null,
			null,
			0,
			20
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_CHAT_ROOM_TYPE);

		assertThatThrownBy(() -> chatRoomService.getChatRooms(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			null,
			"UNKNOWN",
			null,
			0,
			20
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_DEPARTMENT);
	}

	@Test
	void getChatMessagesReturnsLatestMessagesInAscendingOrder() {
		TestFixture fixture = createFixture();
		ChatRoom room = saveRoom(fixture.project(), fixture.leader(), "전체 채팅", ChatRoomType.GENERAL, null, false);
		ChatMessage first = chatMessageRepository.save(ChatMessage.text(room, fixture.leader(), "첫 번째 메시지"));
		ChatMessage second = chatMessageRepository.save(ChatMessage.text(room, fixture.member(), "두 번째 메시지"));
		ChatMessage third = chatMessageRepository.save(ChatMessage.text(room, fixture.leader(), "세 번째 메시지"));
		chatMessageRepository.flush();

		ChatMessageListResponse response = chatRoomService.getChatMessages(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			room.getId(),
			null,
			2,
			null
		);

		assertThat(response.projectId()).isEqualTo(fixture.project().getId());
		assertThat(response.chatRoomId()).isEqualTo(room.getId());
		assertThat(response.hasNext()).isTrue();
		assertThat(response.nextCursor()).isEqualTo(second.getId());
		assertThat(response.messages()).extracting(ChatMessageListResponse.ChatMessageResponse::messageId)
			.containsExactly(second.getId(), third.getId());
		assertThat(response.messages().get(1).isMine()).isTrue();
		assertThat(first.getId()).isNotNull();
	}

	@Test
	void getChatMessagesReturnsEmptyArrayWhenRoomHasNoMessages() {
		TestFixture fixture = createFixture();
		ChatRoom room = saveRoom(fixture.project(), fixture.leader(), "전체 채팅", ChatRoomType.GENERAL, null, false);

		ChatMessageListResponse response = chatRoomService.getChatMessages(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			room.getId(),
			null,
			null,
			null
		);

		assertThat(response.hasNext()).isFalse();
		assertThat(response.nextCursor()).isNull();
		assertThat(response.messages()).isEmpty();
	}

	@Test
	void getChatMessagesSupportsBeforeMessageIdCursor() {
		TestFixture fixture = createFixture();
		ChatRoom room = saveRoom(fixture.project(), fixture.leader(), "전체 채팅", ChatRoomType.GENERAL, null, false);
		ChatMessage first = chatMessageRepository.save(ChatMessage.text(room, fixture.leader(), "첫 번째 메시지"));
		ChatMessage second = chatMessageRepository.save(ChatMessage.text(room, fixture.member(), "두 번째 메시지"));
		chatMessageRepository.save(ChatMessage.text(room, fixture.leader(), "세 번째 메시지"));
		chatMessageRepository.flush();

		ChatMessageListResponse response = chatRoomService.getChatMessages(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			room.getId(),
			second.getId(),
			30,
			null
		);

		assertThat(response.hasNext()).isFalse();
		assertThat(response.messages()).extracting(ChatMessageListResponse.ChatMessageResponse::messageId)
			.containsExactly(first.getId());
	}

	@Test
	void sendChatMessageCreatesTextMessage() {
		TestFixture fixture = createFixture();
		ChatRoom room = saveRoom(fixture.project(), fixture.leader(), "전체 채팅", ChatRoomType.GENERAL, null, false);

		ChatMessageSendResponse response = chatRoomService.sendChatMessage(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			room.getId(),
			new ChatMessageSendRequest("오늘 프로젝트 일정 API 작업 완료했습니다.", null)
		);

		assertThat(response.messageId()).isNotNull();
		assertThat(response.messageType()).isEqualTo(ChatMessageType.TEXT);
		assertThat(response.senderId()).isEqualTo(fixture.leader().getId());
	}

	@Test
	void sendChatMessageRejectsBlankAndTooLongContent() {
		TestFixture fixture = createFixture();
		ChatRoom room = saveRoom(fixture.project(), fixture.leader(), "전체 채팅", ChatRoomType.GENERAL, null, false);

		assertThatThrownBy(() -> chatRoomService.sendChatMessage(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			room.getId(),
			new ChatMessageSendRequest(" ", null)
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);

		assertThatThrownBy(() -> chatRoomService.sendChatMessage(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			room.getId(),
			new ChatMessageSendRequest("a".repeat(2001), null)
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG);
	}

	@Test
	void uploadChatFileCreatesImageMessage() {
		TestFixture fixture = createFixture();
		ChatRoom room = saveRoom(fixture.project(), fixture.leader(), "전체 채팅", ChatRoomType.GENERAL, null, false);
		MockMultipartFile file = new MockMultipartFile("file", "sample.png", "image/png", "image".getBytes());

		ChatFileUploadResponse response = chatRoomService.uploadChatFile(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			room.getId(),
			file,
			"화면 캡처 공유합니다."
		);

		assertThat(response.messageId()).isNotNull();
		assertThat(response.messageType()).isEqualTo(ChatMessageType.IMAGE);
		assertThat(response.fileUrl()).contains("/uploads/chat/" + fixture.project().getId() + "/" + room.getId());
		assertThat(response.originalFileName()).isEqualTo("sample.png");
	}

	@Test
	void uploadChatFileRejectsMissingAndUnsupportedFile() {
		TestFixture fixture = createFixture();
		ChatRoom room = saveRoom(fixture.project(), fixture.leader(), "전체 채팅", ChatRoomType.GENERAL, null, false);
		MockMultipartFile unsupported = new MockMultipartFile(
			"file",
			"malware.exe",
			"application/octet-stream",
			"binary".getBytes()
		);

		assertThatThrownBy(() -> chatRoomService.uploadChatFile(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			room.getId(),
			null,
			null
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CHAT_FILE_REQUIRED);

		assertThatThrownBy(() -> chatRoomService.uploadChatFile(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			room.getId(),
			unsupported,
			null
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CHAT_FILE_TYPE_NOT_ALLOWED);
	}

	@Test
	void chatMessageApisRejectNonProjectMemberAndMissingRoom() {
		TestFixture fixture = createFixture();
		ChatRoom room = saveRoom(fixture.project(), fixture.leader(), "전체 채팅", ChatRoomType.GENERAL, null, false);

		assertThatThrownBy(() -> chatRoomService.getChatMessages(
			fixture.outsider().getEmail(),
			fixture.project().getId(),
			room.getId(),
			null,
			null,
			null
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);

		assertThatThrownBy(() -> chatRoomService.getChatMessages(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			999_999L,
			null,
			null,
			null
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
	}

	@Test
	void createGeneralChatRoomAddsCreatorAsActiveMember() {
		TestFixture fixture = createFixture();

		ChatRoomCreateResponse response = chatRoomService.createChatRoom(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			new ChatRoomCreateRequest(" API 질문방 ", "GENERAL", null)
		);

		assertThat(response.name()).isEqualTo("API 질문방");
		assertThat(response.type()).isEqualTo(ChatRoomType.GENERAL);
		assertThat(response.department()).isNull();
		assertThat(response.isDefault()).isFalse();
		assertThat(response.memberCount()).isEqualTo(1);
		assertThat(chatRoomMemberRepository.findByChatRoom_IdAndUser_Id(response.chatRoomId(), fixture.leader().getId()))
			.get()
			.extracting(ChatRoomMember::getStatus)
			.isEqualTo(ChatRoomMemberStatus.ACTIVE);
	}

	@Test
	void createDefaultChatRoomAddsAllProjectMembersAndRejectsDuplicate() {
		TestFixture fixture = createFixture();

		ChatRoom defaultRoom = chatRoomProjectLifecycleService.createDefaultChatRoom(fixture.project());

		assertThat(defaultRoom.isDefault()).isTrue();
		assertThat(defaultRoom.getType()).isEqualTo(ChatRoomType.GENERAL);
		assertThat(defaultRoom.getDepartment()).isNull();
		assertThat(chatRoomMemberRepository.countByChatRoom_IdAndStatus(
			defaultRoom.getId(),
			ChatRoomMemberStatus.ACTIVE
		)).isEqualTo(2);

		assertThatThrownBy(() -> chatRoomProjectLifecycleService.createDefaultChatRoom(fixture.project()))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DEFAULT_CHAT_ROOM_ALREADY_EXISTS);
	}

	@Test
	void createGeneralChatRoomRejectsDepartment() {
		TestFixture fixture = createFixture();

		assertThatThrownBy(() -> chatRoomService.createChatRoom(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			new ChatRoomCreateRequest("API 질문방", "GENERAL", "BACKEND")
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DEPARTMENT_NOT_ALLOWED_FOR_GENERAL_CHAT_ROOM);
	}

	@Test
	void createDepartmentChatRoomAddsOnlyActiveDepartmentMembersAndRejectsDuplicate() {
		TestFixture fixture = createFixture();

		ChatRoomCreateResponse response = chatRoomService.createChatRoom(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			new ChatRoomCreateRequest("백엔드 채팅방", "DEPARTMENT", "BACKEND")
		);

		ChatRoom room = chatRoomRepository.findById(response.chatRoomId()).orElseThrow();
		assertThat(room.isPrivate()).isTrue();
		assertThat(response.memberCount()).isEqualTo(1);
		assertThat(chatRoomMemberRepository.findByChatRoom_IdAndUser_Id(room.getId(), fixture.leader().getId())).isPresent();
		assertThat(chatRoomMemberRepository.findByChatRoom_IdAndUser_Id(room.getId(), fixture.member().getId())).isEmpty();

		assertThatThrownBy(() -> chatRoomService.createChatRoom(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			new ChatRoomCreateRequest("백엔드 두 번째 방", "DEPARTMENT", "BACKEND")
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DEPARTMENT_CHAT_ROOM_ALREADY_EXISTS);
	}

	@Test
	void createDepartmentChatRoomValidatesDepartmentAndProjectMembership() {
		TestFixture fixture = createFixture();

		assertThatThrownBy(() -> chatRoomService.createChatRoom(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			new ChatRoomCreateRequest("부서방", "DEPARTMENT", null)
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CHAT_ROOM_DEPARTMENT_REQUIRED);

		assertThatThrownBy(() -> chatRoomService.createChatRoom(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			new ChatRoomCreateRequest("데브옵스방", "DEPARTMENT", "DEVOPS")
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_DEPARTMENT_NOT_FOUND);

		assertThatThrownBy(() -> chatRoomService.createChatRoom(
			fixture.outsider().getEmail(),
			fixture.project().getId(),
			new ChatRoomCreateRequest("일반방", "GENERAL", null)
		))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
	}

	@Test
	void getProjectDepartmentsReturnsActiveCountsAndChatRoomExistence() {
		TestFixture fixture = createFixture();
		chatRoomService.createChatRoom(
			fixture.leader().getEmail(),
			fixture.project().getId(),
			new ChatRoomCreateRequest("백엔드 채팅방", "DEPARTMENT", "BACKEND")
		);

		ProjectDepartmentListResponse response = chatRoomService.getProjectDepartments(
			fixture.leader().getEmail(),
			fixture.project().getId()
		);

		assertThat(response.departments())
			.extracting(
				ProjectDepartmentListResponse.DepartmentItem::department,
				ProjectDepartmentListResponse.DepartmentItem::memberCount,
				ProjectDepartmentListResponse.DepartmentItem::chatRoomExists
			)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple(ProjectDepartment.BACKEND, 1L, true),
				org.assertj.core.groups.Tuple.tuple(ProjectDepartment.FRONTEND, 1L, false)
			);
	}

	private TestFixture createFixture() {
		String suffix = UUID.randomUUID().toString();
		User leader = userRepository.save(User.create(
			"chat-leader-" + suffix,
			"password",
			"Leader",
			"chat-leader-" + suffix + "@example.com",
			UserRole.USER
		));
		User member = userRepository.save(User.create(
			"chat-member-" + suffix,
			"password",
			"Member",
			"chat-member-" + suffix + "@example.com",
			UserRole.USER
		));
		User outsider = userRepository.save(User.create(
			"chat-outsider-" + suffix,
			"password",
			"Outsider",
			"chat-outsider-" + suffix + "@example.com",
			UserRole.USER
		));
		Project project = projectRepository.save(Project.create(
			"Chat Project " + suffix.substring(0, 8),
			"Chat room service test project",
			suffix.substring(0, 8).toUpperCase(),
			"C:\\WE_AI\\chat-room-test",
			LocalDate.now(),
			LocalDate.now().plusDays(30),
			leader
		));
		projectMemberRepository.save(ProjectMember.leader(project, leader, ProjectDepartment.BACKEND));
		projectMemberRepository.save(ProjectMember.member(project, member, ProjectDepartment.FRONTEND));
		projectMemberRepository.flush();
		return new TestFixture(project, leader, member, outsider);
	}

	private ChatRoom saveRoom(
		Project project,
		User createdBy,
		String name,
		ChatRoomType type,
		ProjectDepartment department,
		boolean isPrivate
	) {
		return chatRoomRepository.save(ChatRoom.create(
			project,
			name,
			name + " description",
			type,
			department,
			isPrivate,
			createdBy
		));
	}

	private ChatRoomResponse findRoom(ChatRoomListResponse response, Long chatRoomId) {
		return response.chatRooms().stream()
			.filter(chatRoom -> chatRoom.chatRoomId().equals(chatRoomId))
			.findFirst()
			.orElseThrow();
	}

	private record TestFixture(Project project, User leader, User member, User outsider) {
	}
}
