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
