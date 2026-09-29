package com.weai.server.global.security.jwt;

import com.weai.server.domain.chat.service.ChatRoomService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private static final String CHAT_ROOM_TOPIC_PATTERN = "/topic/projects/(\\d+)/chat-rooms/(\\d+)";
	private static final String NOTIFICATION_TOPIC_PATTERN = "/topic/projects/(\\d+)/notifications/(\\d+)";

	private final JwtTokenProvider jwtTokenProvider;
	private final UserService userService;
	private final ChatRoomService chatRoomService;

	public StompAuthChannelInterceptor(
		JwtTokenProvider jwtTokenProvider,
		UserService userService,
		@Lazy ChatRoomService chatRoomService
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.userService = userService;
		this.chatRoomService = chatRoomService;
	}

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) {
			return message;
		}

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			authenticate(accessor);
		} else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			authorizeSubscription(accessor);
		}

		return message;
	}

	private void authenticate(StompHeaderAccessor accessor) {
		String token = resolveToken(accessor.getFirstNativeHeader("Authorization"));
		if (token == null || !jwtTokenProvider.validateToken(token)) {
			throw new MessagingException("Invalid or missing access token for STOMP CONNECT.");
		}
		accessor.setUser(jwtTokenProvider.getAuthentication(token));
	}

	private void authorizeSubscription(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		if (destination == null) {
			return;
		}

		java.util.regex.Matcher chatRoomMatcher = java.util.regex.Pattern.compile(CHAT_ROOM_TOPIC_PATTERN).matcher(destination);
		if (chatRoomMatcher.matches()) {
			authorizeChatRoomSubscription(accessor, chatRoomMatcher);
			return;
		}

		java.util.regex.Matcher notificationMatcher = java.util.regex.Pattern.compile(NOTIFICATION_TOPIC_PATTERN).matcher(destination);
		if (notificationMatcher.matches()) {
			authorizeNotificationSubscription(accessor, notificationMatcher);
		}
	}

	private void authorizeChatRoomSubscription(StompHeaderAccessor accessor, java.util.regex.Matcher matcher) {
		User user = requireAuthenticatedUser(accessor);
		Long projectId = Long.valueOf(matcher.group(1));
		Long chatRoomId = Long.valueOf(matcher.group(2));
		chatRoomService.validateChatRoomAccess(projectId, chatRoomId, user.getId());
	}

	// 이전에는 이 패턴 자체가 없어서 알림 토픽 구독은 아무 검증도 거치지 않고 그대로 통과했다 -
	// 로그인한 사용자라면 누구든 다른 사용자의 userId를 넣어 /topic/projects/{p}/notifications/{u}에
	// 구독해 타인의 알림을 도청할 수 있었다 (BOLA). 이 토픽은 사용자 개인 채널이므로 경로의
	// userId가 구독자 본인과 정확히 일치해야만 허용한다.
	private void authorizeNotificationSubscription(StompHeaderAccessor accessor, java.util.regex.Matcher matcher) {
		User user = requireAuthenticatedUser(accessor);
		Long targetUserId = Long.valueOf(matcher.group(2));
		if (!user.getId().equals(targetUserId)) {
			throw new MessagingException("Cannot subscribe to another user's notification topic.");
		}
	}

	private User requireAuthenticatedUser(StompHeaderAccessor accessor) {
		if (accessor.getUser() == null) {
			throw new MessagingException("Unauthenticated STOMP session attempted to subscribe.");
		}
		return userService.getUserEntityByEmail(accessor.getUser().getName());
	}

	private String resolveToken(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
			return null;
		}
		return authorizationHeader.substring(7);
	}
}
