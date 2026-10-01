package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

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
        SendMessageResult result = chatMessageService.send(new SendMessageCommand(ROOM_ID, BUYER_ID, CLIENT_MSG_ID, "안녕하세요"));

        // then
        assertThat(result.created()).isTrue();
        assertThat(result.message().getChatRoomId()).isEqualTo(ROOM_ID);
        assertThat(result.message().getSenderId()).isEqualTo(BUYER_ID);
        assertThat(result.message().getContent()).isEqualTo("안녕하세요");
    }

    @DisplayName("[CH-02] 같은 클라이언트 메시지 식별자로 재전송하면 기존 메시지를 돌려준다")
    @Test
    void send_idempotent() {
        // given
        ChatMessage existing = ChatMessage.create(ROOM_ID, BUYER_ID, "안녕하세요", CLIENT_MSG_ID);
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
        given(chatMessageRepository.findByChatRoomIdAndClientMessageId(ROOM_ID, CLIENT_MSG_ID)).willReturn(Optional.of(existing));

        // when
        SendMessageResult result = chatMessageService.send(new SendMessageCommand(ROOM_ID, BUYER_ID, CLIENT_MSG_ID, "안녕하세요"));

        // then
        assertThat(result.created()).isFalse();
        assertThat(result.message()).isSameAs(existing);
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

    @DisplayName("[CH-04] 참여자는 지난 메시지를 최신순 커서로 조회한다")
    @Test
    void getMessages() {
        // given
        ChatMessage m = ChatMessage.create(ROOM_ID, SELLER_ID, "안녕하세요", CLIENT_MSG_ID);
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
        given(chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(
                eq(ROOM_ID), eq(Long.MAX_VALUE), any(PageRequest.class)))
                .willReturn(new SliceImpl<>(List.of(m)));

        // when
        Slice<ChatMessage> slice = chatMessageService.getMessages(ROOM_ID, BUYER_ID, null);

        // then
        assertThat(slice.getContent()).containsExactly(m);
    }

    @DisplayName("[I-08] 참여자가 아니면 메시지를 조회할 수 없다")
    @Test
    void getMessages_notParticipant() {
        // given
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));

        // when & then
        assertThatThrownBy(() -> chatMessageService.getMessages(ROOM_ID, STRANGER_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("[CH-04] 없는 채팅방은 메시지를 조회할 수 없다")
    @Test
    void getMessages_roomNotFound() {
        // given
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatMessageService.getMessages(ROOM_ID, BUYER_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());
    }

    @DisplayName("[CH-06] 읽음 처리하면 내 읽음 위치가 마지막 메시지로 오른다")
    @Test
    void markRead() {
        // given
        ChatRoom room = room();
        ChatMessage latest = ChatMessage.create(ROOM_ID, SELLER_ID, "마지막", CLIENT_MSG_ID);
        ReflectionTestUtils.setField(latest, "id", 9L);
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(chatMessageRepository.findFirstByChatRoomIdOrderByIdDesc(ROOM_ID)).willReturn(Optional.of(latest));

        // when
        ReadReceiptResult result = chatMessageService.markRead(ROOM_ID, BUYER_ID);

        // then
        assertThat(result.readerId()).isEqualTo(BUYER_ID);
        assertThat(result.lastReadMessageId()).isEqualTo(9L);
        assertThat(room.lastReadMessageIdOf(BUYER_ID)).isEqualTo(9L);
        then(chatRoomRepository).should().save(room);
    }

    @DisplayName("[CH-06] 메시지가 없는 방을 읽음 처리하면 읽음 위치는 비어 있다")
    @Test
    void markRead_noMessages() {
        // given
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));
        given(chatMessageRepository.findFirstByChatRoomIdOrderByIdDesc(ROOM_ID)).willReturn(Optional.empty());

        // when
        ReadReceiptResult result = chatMessageService.markRead(ROOM_ID, BUYER_ID);

        // then
        assertThat(result.lastReadMessageId()).isNull();
        then(chatRoomRepository).should(never()).save(any());
    }

    @DisplayName("[I-08] 참여자가 아니면 읽음 처리할 수 없다")
    @Test
    void markRead_notParticipant() {
        // given
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room()));

        // when & then
        assertThatThrownBy(() -> chatMessageService.markRead(ROOM_ID, STRANGER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("[CH-06] 없는 채팅방은 읽음 처리할 수 없다")
    @Test
    void markRead_roomNotFound() {
        // given
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatMessageService.markRead(ROOM_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());
    }

    private ChatRoom room() {
        return ChatRoom.open(99L, SELLER_ID, BUYER_ID);
    }
}
