package project.kjhjdh.ibid.chat.presentation;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.application.TokenProvider;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.chat.application.ChatRoomService;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.exception.GlobalException;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    static final String TOPIC_ROOM_PREFIX = "/topic/room.";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final ChatRoomService chatRoomService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor));
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }
        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor);
        }

        return message;
    }

    private Principal authenticate(StompHeaderAccessor accessor) {
        UserInfo userInfo = tokenProvider.parseAccessToken(bearerToken(accessor));
        return new StompPrincipal(userInfo.userId());
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(TOPIC_ROOM_PREFIX)) {
            return;
        }
        Long roomId = parseRoomId(destination);
        Long userId = currentUserId(accessor);
        if (!chatRoomService.isParticipant(roomId, userId)) {
            throw new GlobalException(ErrorCode.ACCESS_DENIED);
        }
    }

    private String bearerToken(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader("Authorization");
        if (headers == null || headers.isEmpty() || !headers.get(0).startsWith(BEARER_PREFIX)) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }
        return headers.get(0).substring(BEARER_PREFIX.length());
    }

    private Long currentUserId(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (!(user instanceof StompPrincipal principal)) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }

    private Long parseRoomId(String destination) {
        try {
            return Long.parseLong(destination.substring(TOPIC_ROOM_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new GlobalException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }

    record StompPrincipal(Long userId) implements Principal {
        @Override
        public String getName() {
            return String.valueOf(userId);
        }
    }
}
