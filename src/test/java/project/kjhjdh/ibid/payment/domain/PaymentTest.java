package project.kjhjdh.ibid.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

class PaymentTest {

    private static final Long ORDER_ID = 1L;
    private static final long TOTAL_AMOUNT = 50000L;

    @DisplayName("결제를 준비하면 READY 상태로 생성된다")
    @Test
    void ready() {
        // when
        Payment payment = Payment.ready(ORDER_ID, TOTAL_AMOUNT);

        // then
        assertThat(payment.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(payment.getTotalAmount()).isEqualTo(TOTAL_AMOUNT);
        assertThat(payment.getState()).isEqualTo(State.READY);
        assertThat(payment.getPaymentKey()).isNull();
    }

    @DisplayName("준비된 결제를 승인하면 CONFIRMED 상태가 되고 paymentKey가 저장된다")
    @Test
    void confirm() {
        // given
        Payment payment = Payment.ready(ORDER_ID, TOTAL_AMOUNT);

        // when
        payment.confirm("payment-key-1");

        // then
        assertThat(payment.getState()).isEqualTo(State.CONFIRMED);
        assertThat(payment.getPaymentKey()).isEqualTo("payment-key-1");
    }

    @DisplayName("READY 상태가 아닌 결제는 승인할 수 없다")
    @Test
    void confirm_notReady() {
        // given
        Payment payment = Payment.ready(ORDER_ID, TOTAL_AMOUNT);
        payment.confirm("payment-key-1");

        // when & then
        assertThatThrownBy(() -> payment.confirm("payment-key-2"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PAYMENT_CONFIRM.getMessage());
    }
}
