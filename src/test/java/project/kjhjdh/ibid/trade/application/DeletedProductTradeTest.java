package project.kjhjdh.ibid.trade.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class DeletedProductTradeTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Long OTHER_USER_ID = 30L;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CompletionService completionService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @DisplayName("[TR-01] 삭제된 상품은 예약할 수 없고 상태는 바뀌지 않는다 (QA-1.1)")
    @Test
    @Disabled("QA-1.1 발견 — 삭제 상품이 예약된다. T-39 에서 막고 이 줄을 지운다")
    void reserve_deletedProduct() {
        // given — 채팅 상대가 있는 상품을 판매자가 삭제한다
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        productService.delete(SELLER_ID, productId);

        // when
        assertThatThrownBy(() -> reservationService.reserve(productId, SELLER_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());

        // then — 숨긴 상품의 상태는 바뀌지 않는다
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getReservedBuyerId()).isNull();
    }

    @DisplayName("[TR-02] 삭제된 상품의 예약은 해제할 수 없고 상태는 바뀌지 않는다 (QA-1.1)")
    @Test
    @Disabled("QA-1.1 발견 — 삭제 상품의 예약이 해제된다. T-39 에서 막고 이 줄을 지운다")
    void cancelReservation_deletedProduct() {
        // given — 예약중인 상품을 판매자가 삭제한다
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        reservationService.reserve(productId, SELLER_ID, BUYER_ID);
        productService.delete(SELLER_ID, productId);

        // when
        assertThatThrownBy(() -> reservationService.cancelReservation(productId, SELLER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());

        // then — 예약중으로 동결된다 (ADR-0013 이 바꾸지 않는 부분)
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.RESERVED);
        assertThat(product.getReservedBuyerId()).isEqualTo(BUYER_ID);
    }

    @DisplayName("[TR-03] 삭제된 상품은 거래완료할 수 없고 상태는 바뀌지 않는다 (QA-1.2)")
    @Test
    @Disabled("QA-1.2 발견 — 삭제 상품이 거래완료된다. T-39 에서 막고 이 줄을 지운다")
    void complete_deletedProduct() {
        // given
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        productService.delete(SELLER_ID, productId);

        // when
        assertThatThrownBy(() -> completionService.complete(productId, SELLER_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());

        // then
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getSoldBuyerId()).isNull();
    }

    @DisplayName("[I-13] 삭제된 상품은 판매자가 아닌 사람에게도 404 다 (삭제된 상품의 존재를 새 쓰기가 알려 주지 않는다)")
    @Test
    @Disabled("QA-1.1 발견 — 삭제 상품이 판매자가 아닌 사람에게 403 으로 존재를 알린다. T-39 에서 404 로 막고 이 줄을 지운다")
    void trade_deletedProduct_notOwner() {
        // given
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        productService.delete(SELLER_ID, productId);

        // when & then — 판매자가 아니어도 403 이 아니라 404 다
        assertThatThrownBy(() -> reservationService.reserve(productId, OTHER_USER_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        assertThatThrownBy(() -> completionService.complete(productId, OTHER_USER_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @DisplayName("[I-13] 삭제된 상품에 예약 · 해제 · 거래완료를 차례로 시도하면 성공하는 연산이 없고 상태가 그대로다 (QA-1.1 · 1.2 연쇄)")
    @Test
    @Disabled("QA-1.1 · 1.2 발견 — 삭제 상품에 3개 연산이 모두 성공해 SOLD 가 된다(실측 3/3). T-39 에서 막고 이 줄을 지운다")
    void trade_deletedProduct_noOperationSucceeds() {
        // given — 채팅 상대가 있는 상품을 판매자가 삭제한다
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        productService.delete(SELLER_ID, productId);

        // when — 예약 → 해제 → 거래완료를 차례로 시도한다
        int succeeded = succeeds(() -> reservationService.reserve(productId, SELLER_ID, BUYER_ID))
                + succeeds(() -> reservationService.cancelReservation(productId, SELLER_ID))
                + succeeds(() -> completionService.complete(productId, SELLER_ID, BUYER_ID));

        // then — 성공한 연산이 없고 숨긴 상품의 상태는 처음 그대로다
        assertThat(succeeded).as("성공한 연산 수 (3 개 중). 최종 상태: %s", stateOf(productId)).isZero();
        assertThat(stateOf(productId)).isEqualTo("status=ON_SALE deleted=true reservedBuyerId=null soldBuyerId=null");
    }

    @DisplayName("[TR-01] 거래완료된 상품은 예약할 수 없다 (QA-1.6 대조군 — 엔티티 상태 가드)")
    @Test
    void reserve_soldProduct() {
        // given — 거래완료된 상품
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        completionService.complete(productId, SELLER_ID, BUYER_ID);

        // when
        assertThatThrownBy(() -> reservationService.reserve(productId, SELLER_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_ON_SALE.getMessage());

        // then
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(product.getReservedBuyerId()).isNull();
    }

    @DisplayName("[I-04] 다른 상품의 채팅 상대는 예약할 수 없다 (QA-3.5)")
    @Test
    void reserve_chatPartnerOfAnotherProduct() {
        // given — 구매자는 상품 A 에만 채팅방이 있다
        Long productA = saveProduct();
        Long productB = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productA, SELLER_ID, BUYER_ID));

        // when — 상품 B 에 그 구매자를 예약한다
        assertThatThrownBy(() -> reservationService.reserve(productB, SELLER_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_CHAT_PARTNER.getMessage());

        // then
        Product product = productRepository.findById(productB).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getReservedBuyerId()).isNull();
    }

    private int succeeds(Runnable operation) {
        try {
            operation.run();
            return 1;
        } catch (BusinessException expected) {
            return 0;
        }
    }

    private String stateOf(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        return "status=%s deleted=%s reservedBuyerId=%s soldBuyerId=%s"
                .formatted(product.getStatus(), product.isDeleted(), product.getReservedBuyerId(), product.getSoldBuyerId());
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
