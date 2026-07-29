package project.kjhjdh.ibid.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.payment.domain.Payment;
import project.kjhjdh.ibid.payment.domain.State;
import project.kjhjdh.ibid.payment.infra.PaymentRepository;
import project.kjhjdh.ibid.payment.infra.dto.PaymentTossDtoImpl;
import project.kjhjdh.ibid.product.application.ProductStockHandler;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ProductStockHandler productStockHandler;

	@InjectMocks
	private PaymentProcessor paymentProcessor;

	@DisplayName("승인 결과를 반영하면 Payment가 CONFIRMED 상태가 되고 paymentKey가 저장되며 주문이 PAID 상태가 되고 재고가 감소한다")
	@Test
	void success() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		ReflectionTestUtils.setField(payment, "id", 1L);
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));

		Order order = Order.create(1L, 2L, 3L, 4, 50000);
		given(orderRepository.findById(10L)).willReturn(Optional.of(order));

		// when
		paymentProcessor.success(1L, tossDto("pk-1"));

		// then
		assertThat(payment.getState()).isEqualTo(State.CONFIRMED);
		assertThat(payment.getPaymentKey()).isEqualTo("pk-1");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		verify(productStockHandler).decreaseStock(1L, 4);
	}

	@DisplayName("준비된 결제가 없으면 PAYMENT_NOT_FOUND 예외를 던진다")
	@Test
	void success_paymentNotFound() {
		// given
		given(paymentRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentProcessor.success(999L, tossDto("pk-1")))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
	}

	@DisplayName("이미 승인된 결제를 다시 반영하려 하면 INVALID_PAYMENT_CONFIRM 예외를 던진다")
	@Test
	void success_alreadyConfirmed() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		ReflectionTestUtils.setField(payment, "id", 1L);
		payment.confirm("pk-1");
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));

		// when & then
		assertThatThrownBy(() -> paymentProcessor.success(1L, tossDto("pk-2")))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.INVALID_PAYMENT_CONFIRM.getMessage());
	}

	private PaymentTossDtoImpl tossDto(String paymentKey) {
		PaymentTossDtoImpl dto = new PaymentTossDtoImpl();
		dto.setPaymentKey(paymentKey);
		return dto;
	}
}
