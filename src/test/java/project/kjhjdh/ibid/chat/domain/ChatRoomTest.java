package project.kjhjdh.ibid.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

class ChatRoomTest {

    private static final Long PRODUCT_ID = 99L;
    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Long STRANGER_ID = 30L;

    @DisplayName("[CH-05] 참여자의 읽음 위치는 세팅 전에는 없다")
    @Test
    void lastReadMessageIdOf_default() {
        // given
        ChatRoom room = ChatRoom.open(PRODUCT_ID, SELLER_ID, BUYER_ID);

        // when & then
        assertThat(room.lastReadMessageIdOf(SELLER_ID)).isNull();
        assertThat(room.lastReadMessageIdOf(BUYER_ID)).isNull();
    }

    @DisplayName("[CH-05] 참여자별로 자기 읽음 위치를 돌려준다")
    @Test
    void lastReadMessageIdOf_perParticipant() {
        // given
        ChatRoom room = ChatRoom.open(PRODUCT_ID, SELLER_ID, BUYER_ID);
        ReflectionTestUtils.setField(room, "sellerLastReadMessageId", 7L);
        ReflectionTestUtils.setField(room, "buyerLastReadMessageId", 4L);

        // when & then
        assertThat(room.lastReadMessageIdOf(SELLER_ID)).isEqualTo(7L);
        assertThat(room.lastReadMessageIdOf(BUYER_ID)).isEqualTo(4L);
    }

    @DisplayName("[CH-05] 참여자가 아니면 읽음 위치를 물을 수 없다")
    @Test
    void lastReadMessageIdOf_notParticipant() {
        // given
        ChatRoom room = ChatRoom.open(PRODUCT_ID, SELLER_ID, BUYER_ID);

        // when & then
        assertThatThrownBy(() -> room.lastReadMessageIdOf(STRANGER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }
}
