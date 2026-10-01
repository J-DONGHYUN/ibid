package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatMessageRepository;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ChatListIntegrationTest extends IntegrationTestSupport {

    private static final Long BUYER_ID = 100L;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[CH-05] 내 채팅방은 마지막 메시지 최신순으로 나오고, 커서로 이어 받으면 겹치거나 빠지지 않는다")
    @Test
    void getMyRooms_orderAndCursor() {
        // given — 두 방 A·B, 메시지를 A→B→A 순으로 넣어 A 의 마지막이 더 최신
        Long sellerA = 10L;
        Long sellerB = 11L;
        Long roomA = openRoom(sellerA, BUYER_ID);
        Long roomB = openRoom(sellerB, BUYER_ID);
        send(roomA, sellerA, "a1");
        send(roomB, sellerB, "b1");
        Long lastOfA = send(roomA, sellerA, "a2").getId();

        // when — 첫 페이지
        Slice<MyChatRoomResult> first = chatRoomService.getMyRooms(BUYER_ID, null);

        // then — A(최신) 다음 B
        assertThat(first.getContent()).extracting(MyChatRoomResult::chatRoomId)
                .containsExactly(roomA, roomB);

        // when — A 의 마지막 메시지 id 를 커서로 이어 받으면
        Slice<MyChatRoomResult> second = chatRoomService.getMyRooms(BUYER_ID, lastOfA);

        // then — A 는 빠지고 B 만, 겹침 없이
        assertThat(second.getContent()).extracting(MyChatRoomResult::chatRoomId)
                .containsExactly(roomB);
    }

    @DisplayName("[CH-05] 안 읽은 수는 내 읽음 위치 이후 상대가 보낸 메시지 수다")
    @Test
    void getMyRooms_unreadCount() {
        // given
        Long seller = 10L;
        Long roomId = openRoom(seller, BUYER_ID);
        Long firstFromSeller = send(roomId, seller, "안녕하세요").getId();
        send(roomId, seller, "계신가요");
        markRead(roomId, firstFromSeller);

        // when
        Slice<MyChatRoomResult> result = chatRoomService.getMyRooms(BUYER_ID, null);

        // then — 첫 메시지까지 읽었으니 안 읽은 수는 1
        assertThat(result.getContent().get(0).unreadCount()).isEqualTo(1L);
    }

    @DisplayName("[CH-04] 메시지를 최신순 커서로 이어 받으면 겹치거나 빠지지 않는다")
    @Test
    void getMessages_cursor() {
        // given
        Long seller = 10L;
        Long roomId = openRoom(seller, BUYER_ID);
        Long m1 = send(roomId, seller, "하나").getId();
        Long m2 = send(roomId, BUYER_ID, "둘").getId();
        Long m3 = send(roomId, seller, "셋").getId();

        // when & then — 최신순
        Slice<ChatMessage> page = chatRoomMessages(roomId, null);
        assertThat(page.getContent()).extracting(ChatMessage::getId).containsExactly(m3, m2, m1);

        // 커서로 m2 이전만
        Slice<ChatMessage> next = chatRoomMessages(roomId, m2);
        assertThat(next.getContent()).extracting(ChatMessage::getId).containsExactly(m1);
    }

    @DisplayName("[CH-07] 판매자는 내 상품의 채팅방들을 방 id 역순 커서로 조회한다 — 메시지 없는 방도 포함, 겹침·빠짐 없음")
    @Test
    void getProductRooms_includesEmptyAndCursor() {
        // given — 한 상품에 두 구매 희망자가 방을 열고, 한 방만 메시지가 있다
        Long seller = 10L;
        Long productId = productRepository.save(Product.create(
                seller, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        Long roomA = chatRoomRepository.save(ChatRoom.open(productId, seller, 100L)).getId();
        Long roomB = chatRoomRepository.save(ChatRoom.open(productId, seller, 200L)).getId();
        send(roomA, 100L, "a1");

        // when — 첫 페이지 (방 id 역순)
        Slice<ProductChatRoomResult> first = chatRoomService.getProductRooms(productId, seller, null);

        // then — 메시지 없는 roomB 도 포함, 역순
        assertThat(first.getContent()).extracting(ProductChatRoomResult::chatRoomId)
                .containsExactly(roomB, roomA);
        assertThat(first.getContent().get(0).lastMessage()).as("메시지 없는 방").isNull();

        // when — roomB 를 커서로 이어 받으면
        Slice<ProductChatRoomResult> second = chatRoomService.getProductRooms(productId, seller, roomB);

        // then — roomB 는 빠지고 roomA 만
        assertThat(second.getContent()).extracting(ProductChatRoomResult::chatRoomId)
                .containsExactly(roomA);
    }

    @DisplayName("[CH-07] 판매자 본인이 아니면 상품별 채팅방을 조회할 수 없다")
    @Test
    void getProductRooms_notOwner() {
        // given
        Long seller = 10L;
        Long productId = productRepository.save(Product.create(
                seller, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        chatRoomRepository.save(ChatRoom.open(productId, seller, 100L));

        // when & then — 다른 사용자
        assertThatThrownBy(() -> chatRoomService.getProductRooms(productId, 999L, null))
                .isInstanceOf(BusinessException.class);
    }

    private Slice<ChatMessage> chatRoomMessages(Long roomId, Long cursor) {
        return chatMessageService.getMessages(roomId, BUYER_ID, cursor).messages();
    }

    private Long openRoom(Long sellerId, Long buyerId) {
        Product product = productRepository.save(Product.create(
                sellerId, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample()));
        return chatRoomRepository.save(ChatRoom.open(product.getId(), sellerId, buyerId)).getId();
    }

    private ChatMessage send(Long roomId, Long senderId, String clientMessageId) {
        return chatMessageService.send(
                new SendMessageCommand(roomId, senderId, clientMessageId, "내용")).message();
    }

    private void markRead(Long roomId, Long lastReadMessageId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        ReflectionTestUtils.setField(room, "buyerLastReadMessageId", lastReadMessageId);
        chatRoomRepository.save(room);
    }
}
