package project.kjhjdh.ibid.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.payment.domain.Payment;
import project.kjhjdh.ibid.payment.domain.State;
import project.kjhjdh.ibid.payment.infra.PaymentRepository;
import project.kjhjdh.ibid.payment.infra.dto.PaymentTossDtoImpl;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateResponse;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private PaymentTossConfirmHandler paymentTossConfirmHandler;

	@Mock
	private PaymentProcessor paymentProcessor;

	@InjectMocks
	private PaymentService paymentService;

	@DisplayName("결제를 생성하면 주문 금액으로 READY 상태의 Payment를 저장하고 paymentId를 반환한다")
	@Test
	void create() {
		// given
		Order order = Order.create(1L, 2L, 3L, 1, 50000);
		given(orderRepository.findById(10L)).willReturn(Optional.of(order));
		Payment saved = Payment.ready(10L, 50000L);
		ReflectionTestUtils.setField(saved, "id", 1L);
		given(paymentRepository.save(any(Payment.class))).willReturn(saved);

		// when
		PaymentCreateResponse response = paymentService.create(new PaymentCreateRequest(10L));

		// then
		assertThat(response.paymentId()).isEqualTo(1L);

		ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(captor.capture());
		assertThat(captor.getValue().getState()).isEqualTo(State.READY);
		assertThat(captor.getValue().getOrderId()).isEqualTo(10L);
		assertThat(captor.getValue().getTotalAmount()).isEqualTo(50000L);
	}

	@DisplayName("존재하지 않는 주문으로 결제를 생성하면 ORDER_NOT_FOUND 예외를 던진다")
	@Test
	void create_orderNotFound() {
		// given
		given(orderRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentService.create(new PaymentCreateRequest(999L)))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
	}

	@DisplayName("결제를 승인하면 토스 승인 핸들러와 결제 처리기에 위임하고 승인 결과를 반환한다")
	@Test
	void confirm() {
		// given
		PaymentConfirmRequest request = new PaymentConfirmRequest("toss-order-1", "50000", "pk-1");
		PaymentTossDtoImpl tossResponse = new PaymentTossDtoImpl();
		tossResponse.setPaymentKey("pk-1");
		given(paymentTossConfirmHandler.confirm(eq(1L), any())).willReturn(tossResponse);

		// when
		PaymentConfirmResponse result = paymentService.confirm(1L, request);

		// then
		assertThat(result).isSameAs(tossResponse);
		verify(paymentTossConfirmHandler).confirm(eq(1L), any());
		verify(paymentProcessor).success(1L, tossResponse);
	}

	@DisplayName("토스 승인 핸들러가 실패하면 결제 처리기를 호출하지 않고 예외를 전파한다")
	@Test
	void confirm_handlerFailed() {
		// given
		PaymentConfirmRequest request = new PaymentConfirmRequest("toss-order-1", "50000", "pk-1");
		given(paymentTossConfirmHandler.confirm(eq(1L), any()))
				.willThrow(new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED));

		// when & then
		assertThatThrownBy(() -> paymentService.confirm(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.PAYMENT_CONFIRM_FAILED.getMessage());

		verify(paymentProcessor, never()).success(any(), any());
	}
}
