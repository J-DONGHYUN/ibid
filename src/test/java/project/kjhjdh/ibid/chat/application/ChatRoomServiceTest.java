package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;
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
import project.kjhjdh.ibid.chat.infra.RoomLastMessage;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.application.ProductService;
import project.kjhjdh.ibid.product.application.ProductSummary;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long OTHER_BUYER_ID = 99L;
    private static final Long BUYER_ID = 20L;
    private static final Long ROOM_ID = 5L;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @DisplayName("[CH-01] 구매 희망자가 상품에 채팅방을 열면 상품·판매자·구매자로 방이 생긴다")
    @Test
    void open() {
        // given
        given(productRepository.findActiveById(PRODUCT_ID)).willReturn(Optional.of(product()));
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, BUYER_ID)).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatRoom room = chatRoomService.open(PRODUCT_ID, BUYER_ID);

        // then
        assertThat(room.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(room.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(room.getBuyerId()).isEqualTo(BUYER_ID);
    }

    @DisplayName("[CH-01] 이미 열린 방이 있으면 새로 만들지 않고 기존 방을 돌려준다")
    @Test
    void open_idempotent() {
        // given
        ChatRoom existing = ChatRoom.open(PRODUCT_ID, SELLER_ID, BUYER_ID);
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, BUYER_ID)).willReturn(Optional.of(existing));

        // when
        ChatRoom room = chatRoomService.open(PRODUCT_ID, BUYER_ID);

        // then
        assertThat(room).isSameAs(existing);
        then(chatRoomRepository).should(never()).save(any());
        then(productRepository).shouldHaveNoInteractions();
    }

    @DisplayName("[I-05] 판매자 본인은 자기 상품에 채팅방을 열 수 없다")
    @Test
    void open_ownProduct() {
        // given
        given(productRepository.findActiveById(PRODUCT_ID)).willReturn(Optional.of(product()));
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, SELLER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomService.open(PRODUCT_ID, SELLER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CANNOT_OPEN_CHAT_ON_OWN_PRODUCT.getMessage());
    }

    @DisplayName("[CH-01] 거래완료된 상품에는 새 채팅방을 열 수 없다")
    @Test
    void open_soldProduct() {
        // given
        Product sold = product();
        sold.complete(BUYER_ID);
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, OTHER_BUYER_ID)).willReturn(Optional.empty());
        given(productRepository.findActiveById(PRODUCT_ID)).willReturn(Optional.of(sold));

        // when & then
        assertThatThrownBy(() -> chatRoomService.open(PRODUCT_ID, OTHER_BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_ALREADY_SOLD.getMessage());
        then(chatRoomRepository).should(never()).save(any());
    }

    @DisplayName("[CH-01] 존재하지 않거나 삭제된 상품에는 새 채팅방을 열 수 없다")
    @Test
    void open_productNotFound() {
        // given — 삭제된 상품은 쓰기 경로 전용 조회가 돌려주지 않는다
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, BUYER_ID)).willReturn(Optional.empty());
        given(productRepository.findActiveById(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomService.open(PRODUCT_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @DisplayName("[CH-05] 내 채팅방마다 상대·상품·마지막 메시지·안 읽은 수가 실린다")
    @Test
    void getMyRooms() {
        // given — 내가 구매 희망자인 방, 내 읽음 위치는 2
        ChatRoom room = ChatRoom.open(PRODUCT_ID, SELLER_ID, BUYER_ID);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        ReflectionTestUtils.setField(room, "buyerLastReadMessageId", 2L);
        ChatMessage lastMessage = ChatMessage.create(ROOM_ID, SELLER_ID, "마지막", "c-3");
        ReflectionTestUtils.setField(lastMessage, "id", 3L);

        given(chatRoomRepository.findMyRoomsOrderByLastMessageDesc(eq(BUYER_ID), any(), any()))
                .willReturn(new SliceImpl<>(List.of(roomLastMessage(ROOM_ID, 3L)), PageRequest.of(0, 20), false));
        given(chatRoomRepository.findAllById(List.of(ROOM_ID))).willReturn(List.of(room));
        given(chatMessageRepository.findAllById(List.of(3L))).willReturn(List.of(lastMessage));
        given(productService.findSummaries(List.of(PRODUCT_ID)))
                .willReturn(Map.of(PRODUCT_ID, new ProductSummary(PRODUCT_ID, "아이폰 13", "https://thumb")));
        given(chatMessageRepository.countByChatRoomIdAndIdGreaterThanAndSenderIdNot(ROOM_ID, 2L, BUYER_ID))
                .willReturn(4L);

        // when
        Slice<MyChatRoomResult> result = chatRoomService.getMyRooms(BUYER_ID, null);

        // then
        MyChatRoomResult room0 = result.getContent().get(0);
        assertThat(room0.chatRoomId()).isEqualTo(ROOM_ID);
        assertThat(room0.peerId()).isEqualTo(SELLER_ID);
        assertThat(room0.product().title()).isEqualTo("아이폰 13");
        assertThat(room0.lastMessage().getId()).isEqualTo(3L);
        assertThat(room0.unreadCount()).isEqualTo(4L);
    }

    @DisplayName("[CH-05] 읽음 위치가 없으면 상대가 보낸 메시지 전부가 안 읽은 수다")
    @Test
    void getMyRooms_noReadPositionCountsFromStart() {
        // given — 읽음 위치 미세팅(null)
        ChatRoom room = ChatRoom.open(PRODUCT_ID, SELLER_ID, BUYER_ID);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        ChatMessage lastMessage = ChatMessage.create(ROOM_ID, SELLER_ID, "마지막", "c-3");
        ReflectionTestUtils.setField(lastMessage, "id", 3L);

        given(chatRoomRepository.findMyRoomsOrderByLastMessageDesc(eq(BUYER_ID), any(), any()))
                .willReturn(new SliceImpl<>(List.of(roomLastMessage(ROOM_ID, 3L)), PageRequest.of(0, 20), false));
        given(chatRoomRepository.findAllById(List.of(ROOM_ID))).willReturn(List.of(room));
        given(chatMessageRepository.findAllById(List.of(3L))).willReturn(List.of(lastMessage));
        given(productService.findSummaries(List.of(PRODUCT_ID)))
                .willReturn(Map.of(PRODUCT_ID, new ProductSummary(PRODUCT_ID, "아이폰 13", "https://thumb")));
        given(chatMessageRepository.countByChatRoomIdAndIdGreaterThanAndSenderIdNot(ROOM_ID, 0L, BUYER_ID))
                .willReturn(3L);

        // when
        Slice<MyChatRoomResult> result = chatRoomService.getMyRooms(BUYER_ID, null);

        // then
        assertThat(result.getContent().get(0).unreadCount()).isEqualTo(3L);
    }

    @DisplayName("[CH-07] 판매자는 내 상품의 채팅방들을 상대·마지막 메시지·안 읽은 수와 함께 조회한다")
    @Test
    void getProductRooms() {
        // given
        ChatRoom room = ChatRoom.open(PRODUCT_ID, SELLER_ID, BUYER_ID);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        ChatMessage lastMessage = ChatMessage.create(ROOM_ID, BUYER_ID, "안녕하세요", "c-1");
        ReflectionTestUtils.setField(lastMessage, "id", 3L);

        given(productRepository.findIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(product()));
        given(chatRoomRepository.findByProductIdAndSellerIdAndIdLessThanOrderByIdDesc(
                eq(PRODUCT_ID), eq(SELLER_ID), any(), any()))
                .willReturn(new SliceImpl<>(List.of(room), PageRequest.of(0, 20), false));
        given(chatMessageRepository.findFirstByChatRoomIdOrderByIdDesc(ROOM_ID))
                .willReturn(Optional.of(lastMessage));
        given(chatMessageRepository.countByChatRoomIdAndIdGreaterThanAndSenderIdNot(ROOM_ID, 0L, SELLER_ID))
                .willReturn(2L);

        // when
        Slice<ProductChatRoomResult> result = chatRoomService.getProductRooms(PRODUCT_ID, SELLER_ID, null);

        // then
        ProductChatRoomResult room0 = result.getContent().get(0);
        assertThat(room0.chatRoomId()).isEqualTo(ROOM_ID);
        assertThat(room0.buyerId()).isEqualTo(BUYER_ID);
        assertThat(room0.lastMessage().getId()).isEqualTo(3L);
        assertThat(room0.unreadCount()).isEqualTo(2L);
    }

    @DisplayName("[CH-07] 판매자 본인이 아니면 상품별 채팅방을 조회할 수 없다")
    @Test
    void getProductRooms_notOwner() {
        // given
        given(productRepository.findIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(product()));

        // when & then — 구매자는 상품 소유자가 아니다
        assertThatThrownBy(() -> chatRoomService.getProductRooms(PRODUCT_ID, BUYER_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("[CH-07] 없는 상품의 채팅방은 조회할 수 없다")
    @Test
    void getProductRooms_productNotFound() {
        // given
        given(productRepository.findIncludingDeleted(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomService.getProductRooms(PRODUCT_ID, SELLER_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @DisplayName("[I-04] 그 상품에 채팅방이 있는 사용자는 채팅 상대다")
    @Test
    void isChatPartner() {
        // given
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, BUYER_ID))
                .willReturn(Optional.of(ChatRoom.open(PRODUCT_ID, SELLER_ID, BUYER_ID)));
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, 999L))
                .willReturn(Optional.empty());

        // when & then
        assertThat(chatRoomService.isChatPartner(PRODUCT_ID, BUYER_ID)).isTrue();
        assertThat(chatRoomService.isChatPartner(PRODUCT_ID, 999L)).isFalse();
    }

    private RoomLastMessage roomLastMessage(Long roomId, Long lastMessageId) {
        return new RoomLastMessage() {
            @Override
            public Long getRoomId() {
                return roomId;
            }

            @Override
            public Long getLastMessageId() {
                return lastMessageId;
            }
        };
    }

    private Product product() {
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000,
                ProductCondition.USED, DeviceSpecFixture.sample());
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }
}
