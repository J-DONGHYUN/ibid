package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatMessageRepository;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Long STRANGER_ID = 30L;
    private static final String CLIENT_MSG_ID = "c-1";

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @DisplayName("[CH-02] 참여자가 메시지를 보내면 저장된다")
    @Test
    void send() {
        // given
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
        given(chatMessageRepository.findByChatRoomIdAndClientMessageId(ROOM_ID, CLIENT_MSG_ID)).willReturn(Optional.empty());
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage message = chatMessageService.send(new SendMessageCommand(ROOM_ID, BUYER_ID, CLIENT_MSG_ID, "안녕하세요"));

        // then
        assertThat(message.getChatRoomId()).isEqualTo(ROOM_ID);
        assertThat(message.getSenderId()).isEqualTo(BUYER_ID);
        assertThat(message.getContent()).isEqualTo("안녕하세요");
    }

    @DisplayName("[CH-02] 같은 클라이언트 메시지 식별자로 재전송하면 기존 메시지를 돌려준다")
    @Test
    void send_idempotent() {
        // given
        ChatMessage existing = ChatMessage.create(ROOM_ID, BUYER_ID, "안녕하세요", CLIENT_MSG_ID);
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
        given(chatMessageRepository.findByChatRoomIdAndClientMessageId(ROOM_ID, CLIENT_MSG_ID)).willReturn(Optional.of(existing));

        // when
        ChatMessage message = chatMessageService.send(new SendMessageCommand(ROOM_ID, BUYER_ID, CLIENT_MSG_ID, "안녕하세요"));

        // then
        assertThat(message).isSameAs(existing);
        then(chatMessageRepository).should(never()).save(any());
    }

    @DisplayName("[I-08] 참여자가 아니면 메시지를 보낼 수 없다")
    @Test
    void send_notParticipant() {
        // given
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));

        // when & then
        assertThatThrownBy(() -> chatMessageService.send(new SendMessageCommand(ROOM_ID, STRANGER_ID, CLIENT_MSG_ID, "안녕하세요")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
        then(chatMessageRepository).shouldHaveNoInteractions();
    }

    @DisplayName("[CH-02] 없는 채팅방에는 메시지를 보낼 수 없다")
    @Test
    void send_roomNotFound() {
        // given
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatMessageService.send(new SendMessageCommand(ROOM_ID, BUYER_ID, CLIENT_MSG_ID, "안녕하세요")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());
    }

    private ChatRoom room() {
        return ChatRoom.open(99L, SELLER_ID, BUYER_ID);
    }
}
