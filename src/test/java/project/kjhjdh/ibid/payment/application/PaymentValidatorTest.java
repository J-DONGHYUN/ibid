package project.kjhjdh.ibid.payment.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.payment.domain.Payment;
import project.kjhjdh.ibid.payment.infra.PaymentRepository;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmRequest;

@ExtendWith(MockitoExtension.class)
class PaymentValidatorTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private OrderRepository orderRepository;

	@InjectMocks
	private PaymentValidator paymentValidator;

	@DisplayName("주문이 CREATED 상태이고 결제가 READY 상태이며 금액이 일치하면 검증을 통과한다")
	@Test
	void validate() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
		Order order = Order.create(1L, 2L, 3L, 1, 50000);
		given(orderRepository.findById(10L)).willReturn(Optional.of(order));

		PaymentConfirmRequest request = new PaymentConfirmRequest("toss-order-1", "50000", "pk-1");

		// when & then
		assertThatCode(() -> paymentValidator.validate(1L, request)).doesNotThrowAnyException();
	}

	@DisplayName("결제를 찾을 수 없으면 PAYMENT_NOT_FOUND 예외를 던진다")
	@Test
	void validate_paymentNotFound() {
		// given
		given(paymentRepository.findById(999L)).willReturn(Optional.empty());

		PaymentConfirmRequest request = new PaymentConfirmRequest("toss-order-1", "50000", "pk-1");

		// when & then
		assertThatThrownBy(() -> paymentValidator.validate(999L, request))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
	}

	@DisplayName("주문을 찾을 수 없으면 ORDER_NOT_FOUND 예외를 던진다")
	@Test
	void validate_orderNotFound() {
		// given
		Payment payment = Payment.ready(999L, 50000L);
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
		given(orderRepository.findById(999L)).willReturn(Optional.empty());

		PaymentConfirmRequest request = new PaymentConfirmRequest("toss-order-1", "50000", "pk-1");

		// when & then
		assertThatThrownBy(() -> paymentValidator.validate(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
	}

	@DisplayName("주문이 CREATED 상태가 아니면 ORDER_NOT_PAYABLE 예외를 던진다")
	@Test
	void validate_orderNotPayable() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
		Order order = Order.create(1L, 2L, 3L, 1, 50000);
		order.confirmPaid();
		given(orderRepository.findById(10L)).willReturn(Optional.of(order));

		PaymentConfirmRequest request = new PaymentConfirmRequest("toss-order-1", "50000", "pk-1");

		// when & then
		assertThatThrownBy(() -> paymentValidator.validate(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.ORDER_NOT_PAYABLE.getMessage());
	}

	@DisplayName("결제가 READY 상태가 아니면 INVALID_PAYMENT_CONFIRM 예외를 던진다")
	@Test
	void validate_paymentNotReady() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		payment.confirm("already-confirmed-key");
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
		Order order = Order.create(1L, 2L, 3L, 1, 50000);
		given(orderRepository.findById(10L)).willReturn(Optional.of(order));

		PaymentConfirmRequest request = new PaymentConfirmRequest("toss-order-1", "50000", "pk-1");

		// when & then
		assertThatThrownBy(() -> paymentValidator.validate(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.INVALID_PAYMENT_CONFIRM.getMessage());
	}

	@DisplayName("요청 금액이 결제 금액과 다르면 PAYMENT_AMOUNT_MISMATCH 예외를 던진다")
	@Test
	void validate_amountMismatch() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
		Order order = Order.create(1L, 2L, 3L, 1, 50000);
		given(orderRepository.findById(10L)).willReturn(Optional.of(order));

		PaymentConfirmRequest request = new PaymentConfirmRequest("toss-order-1", "999999", "pk-1");

		// when & then
		assertThatThrownBy(() -> paymentValidator.validate(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());
	}

	@DisplayName("요청 금액이 숫자 형식이 아니면 INVALID_INPUT 예외를 던진다")
	@Test
	void validate_amountNotNumeric() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
		Order order = Order.create(1L, 2L, 3L, 1, 50000);
		given(orderRepository.findById(10L)).willReturn(Optional.of(order));

		PaymentConfirmRequest request = new PaymentConfirmRequest("toss-order-1", "오만원", "pk-1");

		// when & then
		assertThatThrownBy(() -> paymentValidator.validate(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.INVALID_INPUT.getMessage());
	}
}
