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

		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(CHAT_ROOM_TOPIC_PATTERN).matcher(destination);
		if (!matcher.matches()) {
			return;
		}

		if (accessor.getUser() == null) {
			throw new MessagingException("Unauthenticated STOMP session attempted to subscribe.");
		}

		Long projectId = Long.valueOf(matcher.group(1));
		Long chatRoomId = Long.valueOf(matcher.group(2));
		User user = userService.getUserEntityByEmail(accessor.getUser().getName());
		chatRoomService.validateChatRoomAccess(projectId, chatRoomId, user.getId());
	}

	private String resolveToken(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
			return null;
		}
		return authorizationHeader.substring(7);
	}
}
