package project.kjhjdh.ibid.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

class OrderTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long BUYER_ID = 2L;
    private static final Long SELLER_ID = 3L;

    @DisplayName("주문을 생성하면 총 금액은 단가와 수량의 곱이다")
    @Test
    void create() {
        // when
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);

        // then
        assertThat(order.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(order.getBuyerId()).isEqualTo(BUYER_ID);
        assertThat(order.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(order.getQuantity()).isEqualTo(3);
        assertThat(order.getTotalPrice()).isEqualTo(267000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @DisplayName("생성된 주문의 결제를 확정하면 PAID가 된다")
    @Test
    void confirmPaid() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);

        // when
        order.confirmPaid();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @DisplayName("생성 상태가 아닌 주문은 결제를 확정할 수 없다")
    @Test
    void confirmPaid_notCreated() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);
        order.confirmPaid();

        // when & then
        assertThatThrownBy(order::confirmPaid)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_PAYABLE.getMessage());
    }

    @DisplayName("생성된 주문을 취소하면 CANCELED가 된다")
    @Test
    void cancel() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);

        // when
        order.cancel();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @DisplayName("결제된 주문을 발송하면 검수업체 발송 상태가 된다")
    @Test
    void ship() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);
        order.confirmPaid();

        // when
        order.ship();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED_TO_INSPECTOR);
    }

    @DisplayName("결제되지 않은 주문은 발송할 수 없다")
    @Test
    void ship_notPaid() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);

        // when & then
        assertThatThrownBy(order::ship)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_SHIPPABLE.getMessage());
    }

    @DisplayName("발송된 주문을 검수 시작하면 검수중 상태가 된다")
    @Test
    void startInspection() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);
        order.confirmPaid();
        order.ship();

        // when
        order.startInspection();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.UNDER_INSPECTION);
    }

    @DisplayName("발송되지 않은 주문은 검수를 시작할 수 없다")
    @Test
    void startInspection_notShipped() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);
        order.confirmPaid();

        // when & then
        assertThatThrownBy(order::startInspection)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_INSPECTABLE.getMessage());
    }

    @DisplayName("검수중인 주문을 완료하면 거래 완료 상태가 된다")
    @Test
    void complete() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);
        order.confirmPaid();
        order.ship();
        order.startInspection();

        // when
        order.complete();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @DisplayName("검수중이 아닌 주문은 완료할 수 없다")
    @Test
    void complete_notUnderInspection() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);
        order.confirmPaid();

        // when & then
        assertThatThrownBy(order::complete)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_JUDGEABLE.getMessage());
    }

    @DisplayName("판매자 본인 여부를 판별한다")
    @Test
    void isSeller() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);

        // when & then
        assertThat(order.isSeller(SELLER_ID)).isTrue();
        assertThat(order.isSeller(BUYER_ID)).isFalse();
    }

    @DisplayName("이미 결제된 주문은 취소할 수 없다")
    @Test
    void cancel_afterPaid() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);
        order.confirmPaid();

        // when & then
        assertThatThrownBy(order::cancel)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_CANCELABLE.getMessage());
    }

    @DisplayName("생성 상태가 아닌 주문은 취소할 수 없다")
    @Test
    void cancel_notCreated() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, 89000);
        order.cancel();

        // when & then
        assertThatThrownBy(order::cancel)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_CANCELABLE.getMessage());
    }

    @DisplayName("구매 수량이 1개 미만이면 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void create_invalidQuantity(int quantity) {
        // when & then
        assertThatThrownBy(() -> Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, quantity, 89000))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PURCHASE_QUANTITY.getMessage());
    }

    @DisplayName("단가가 1원 미만이면 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void create_invalidUnitPrice(int unitPrice) {
        // when & then
        assertThatThrownBy(() -> Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 3, unitPrice))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PRODUCT_PRICE.getMessage());
    }
}
