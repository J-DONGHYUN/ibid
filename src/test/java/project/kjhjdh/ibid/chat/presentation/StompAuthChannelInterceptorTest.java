package project.kjhjdh.ibid.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import project.kjhjdh.ibid.auth.application.TokenProvider;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.chat.application.ChatRoomService;
import project.kjhjdh.ibid.common.exception.GlobalException;
import project.kjhjdh.ibid.user.domain.Role;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 7L;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    @DisplayName("[CH-03] CONNECT 에 유효한 토큰이 있으면 사용자 신원이 붙는다")
    @Test
    void connect_withValidToken() {
        // given
        given(tokenProvider.parseAccessToken("good")).willReturn(new UserInfo(USER_ID, Role.USER));
        Message<byte[]> message = connectMessage("Bearer good");

        // when
        Message<?> result = interceptor.preSend(message, null);

        // then
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("1");
    }

    @DisplayName("[CH-03] CONNECT 에 토큰이 없으면 연결이 거부된다")
    @Test
    void connect_withoutToken() {
        // given
        Message<byte[]> message = connectMessage(null);

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(GlobalException.class);
    }

    @DisplayName("[I-08] 참여자면 그 방 토픽을 구독할 수 있다")
    @Test
    void subscribe_participant() {
        // given
        given(chatRoomService.isParticipant(eq(ROOM_ID), anyLong())).willReturn(true);
        Message<byte[]> message = subscribeMessage("/topic/room." + ROOM_ID);

        // when & then (예외 없음)
        interceptor.preSend(message, null);
    }

    @DisplayName("[I-08] 참여자가 아니면 그 방 토픽을 구독할 수 없다")
    @Test
    void subscribe_notParticipant() {
        // given
        given(chatRoomService.isParticipant(eq(ROOM_ID), anyLong())).willReturn(false);
        Message<byte[]> message = subscribeMessage("/topic/room." + ROOM_ID);

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(GlobalException.class);
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new StompAuthChannelInterceptor.StompPrincipal(USER_ID));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
