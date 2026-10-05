package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.application.ProductService;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;
import project.kjhjdh.ibid.trade.application.CompletionService;
import project.kjhjdh.ibid.trade.application.ReservationService;

class ChatRoomOpenRestrictionTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Long OTHER_BUYER_ID = 30L;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private CompletionService completionService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @DisplayName("[CH-01] 삭제된 상품에는 새 채팅방을 열 수 없다 (QA-1.3)")
    @Test
    @Disabled("QA-1.3 발견 — 삭제된 상품에 새 채팅방이 열린다. T-42 에서 404 로 막고 이 줄을 지운다")
    void open_deletedProduct() {
        // given — 판매자가 상품을 삭제한다 (채팅방은 아직 없다)
        Long productId = saveProduct();
        productService.delete(SELLER_ID, productId);

        // when
        assertThatThrownBy(() -> chatRoomService.open(productId, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());

        // then — 방이 생기지 않는다
        assertThat(chatRoomRepository.findByProductIdAndBuyerId(productId, BUYER_ID)).isEmpty();
    }

    @DisplayName("[CH-01] 거래완료된 상품에는 새 채팅방을 열 수 없다 (QA-1.4)")
    @Test
    @Disabled("QA-1.4 발견 — 거래완료된 상품에 새 채팅방이 열린다. T-42 에서 막고 이 줄을 지운다")
    void open_soldProduct() {
        // given — 기존 구매자와 거래완료된 상품
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        completionService.complete(productId, SELLER_ID, BUYER_ID);

        // when — 거래와 무관한 다른 구매자가 채팅방을 연다
        assertThatThrownBy(() -> chatRoomService.open(productId, OTHER_BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_ALREADY_SOLD.getMessage());

        // then
        assertThat(chatRoomRepository.findByProductIdAndBuyerId(productId, OTHER_BUYER_ID)).isEmpty();
    }

    @DisplayName("[CH-01] 내 상품에는 채팅방을 열 수 없다 (QA-1.7)")
    @Test
    void open_ownProduct() {
        // given
        Long productId = saveProduct();

        // when — 판매자 본인이 자기 상품에 채팅방을 연다
        assertThatThrownBy(() -> chatRoomService.open(productId, SELLER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CANNOT_OPEN_CHAT_ON_OWN_PRODUCT.getMessage());

        // then
        assertThat(chatRoomRepository.findByProductIdAndBuyerId(productId, SELLER_ID)).isEmpty();
    }

    @DisplayName("[CH-01] 거래완료 전에 열린 채팅방은 거래완료 뒤에도 그대로 돌아온다 (QA-1.4 대조군)")
    @Test
    void open_existingRoomAfterSold() {
        // given — 거래 상대의 방은 거래완료 전에 이미 열려 있다
        Long productId = saveProduct();
        Long roomId = chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();
        completionService.complete(productId, SELLER_ID, BUYER_ID);

        // when — 같은 구매자가 다시 연다 (멱등)
        ChatRoom room = chatRoomService.open(productId, BUYER_ID);

        // then — 새 방이 아니라 기존 방이다. 인수 조율 대화가 끊기지 않는다
        assertThat(room.getId()).isEqualTo(roomId);
    }

    @DisplayName("[CH-01] 삭제된 상품이라도 이미 방이 있는 구매자의 open 은 그 방을 돌려준다 (ADR-0013 결정 5)")
    @Test
    void open_existingRoomOfDeletedProduct() {
        // given — 방이 열려 있는 상품을 판매자가 삭제한다
        Long productId = saveProduct();
        Long roomId = chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();
        productService.delete(SELLER_ID, productId);

        // when — 같은 구매자가 다시 연다 (멱등)
        ChatRoom room = chatRoomService.open(productId, BUYER_ID);

        // then — 새 방을 막을 뿐 이미 맺어진 방은 돌려준다
        assertThat(room.getId()).isEqualTo(roomId);
    }

    @DisplayName("[CH-01] 예약중인 상품에는 새 채팅방을 열 수 있다 (과차단 방지)")
    @Test
    void open_reservedProduct() {
        // given — 다른 구매자에게 예약된 상품
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        reservationService.reserve(productId, SELLER_ID, BUYER_ID);

        // when — 또 다른 구매자가 문의한다
        ChatRoom room = chatRoomService.open(productId, OTHER_BUYER_ID);

        // then
        assertThat(room.getBuyerId()).isEqualTo(OTHER_BUYER_ID);
    }

    @DisplayName("[CH-01] 삭제 · 거래완료된 상품에 새 구매자들이 열어도 만들어지는 방이 없다 (QA-1.3 · 1.4 측정)")
    @Test
    @Disabled("QA-1.3 · 1.4 발견 — 삭제 · 거래완료 상품에 새 구매자 3명씩 열면 방이 6개 만들어진다(실측 6/6). T-42 에서 막고 이 줄을 지운다")
    void open_blockedProducts_createNoRoom() {
        // given — 삭제된 상품 하나와 거래완료된 상품 하나. 새 구매자는 상품마다 3명이다
        Long deleted = saveProduct();
        productService.delete(SELLER_ID, deleted);
        Long sold = saveProduct();
        chatRoomRepository.save(ChatRoom.open(sold, SELLER_ID, BUYER_ID));
        completionService.complete(sold, SELLER_ID, BUYER_ID);
        List<Long> newBuyers = List.of(101L, 102L, 103L);

        // when — 새 구매자들이 차례로 연다
        newBuyers.forEach(buyer -> attempt(() -> chatRoomService.open(deleted, buyer)));
        newBuyers.forEach(buyer -> attempt(() -> chatRoomService.open(sold, buyer)));

        // then — 새로 만들어진 방이 하나도 없다
        long createdOnDeleted = roomsOf(deleted, newBuyers);
        long createdOnSold = roomsOf(sold, newBuyers);
        assertThat(createdOnDeleted + createdOnSold)
                .as("만들어진 방 수 (삭제된 상품 %d / 3 + 거래완료 상품 %d / 3)", createdOnDeleted, createdOnSold)
                .isZero();
    }

    private void attempt(Runnable operation) {
        try {
            operation.run();
        } catch (BusinessException expected) {
        }
    }

    private long roomsOf(Long productId, List<Long> buyers) {
        return chatRoomRepository.findAll().stream()
                .filter(room -> room.getProductId().equals(productId) && buyers.contains(room.getBuyerId()))
                .count();
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
