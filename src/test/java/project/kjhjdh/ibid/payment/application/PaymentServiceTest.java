package project.kjhjdh.ibid.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import project.kjhjdh.ibid.payment.domain.TossPayment;
import project.kjhjdh.ibid.payment.infra.PaymentRepository;
import project.kjhjdh.ibid.payment.infra.TossPaymentClient;
import project.kjhjdh.ibid.payment.infra.TossPaymentRepository;
import project.kjhjdh.ibid.payment.infra.dto.PaymentTossDtoImpl;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateResponse;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private TossPaymentClient tossPaymentClient;

	@Mock
	private TossPaymentRepository tossPaymentRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private OrderRepository orderRepository;

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

	@DisplayName("결제 승인에 성공하면 TossPayment를 저장하고 Payment를 CONFIRMED로 전이한다")
	@Test
	void confirm() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		ReflectionTestUtils.setField(payment, "id", 1L);
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
		given(tossPaymentClient.requestConfirm(any())).willReturn(tossDto("DONE"));

		// when
		PaymentConfirmResponse result = paymentService.confirm(1L, new PaymentConfirmRequest("toss-order-1", "50000", "pk-1"));

		// then
		assertThat(((PaymentTossDtoImpl) result).getStatus()).isEqualTo("DONE");
		assertThat(payment.getState()).isEqualTo(State.CONFIRMED);
		assertThat(payment.getPaymentKey()).isEqualTo("pk-1");
		verify(tossPaymentRepository).save(any(TossPayment.class));
	}

	@DisplayName("준비된 결제가 없으면 PAYMENT_NOT_FOUND 예외를 던진다")
	@Test
	void confirm_paymentNotFound() {
		// given
		given(paymentRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentService.confirm(999L, new PaymentConfirmRequest("toss-order-1", "50000", "pk-1")))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
	}

	@DisplayName("토스 승인 자체가 실패하면 취소 요청 없이 예외를 전파한다")
	@Test
	void confirm_tossConfirmFailed() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		ReflectionTestUtils.setField(payment, "id", 1L);
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
		given(tossPaymentClient.requestConfirm(any()))
				.willThrow(new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED));

		// when & then
		assertThatThrownBy(() -> paymentService.confirm(1L, new PaymentConfirmRequest("toss-order-1", "50000", "pk-1")))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.PAYMENT_CONFIRM_FAILED.getMessage());

		assertThat(payment.getState()).isEqualTo(State.READY);
		verify(tossPaymentRepository, never()).save(any());
		verify(tossPaymentClient, never()).requestPaymentCancel(anyString(), anyString());
	}

	@DisplayName("승인 성공 후 저장이 실패하면 토스에 취소를 요청하고 예외를 전파한다")
	@Test
	void confirm_saveFailed() {
		// given
		Payment payment = Payment.ready(10L, 50000L);
		ReflectionTestUtils.setField(payment, "id", 1L);
		given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
		given(tossPaymentClient.requestConfirm(any())).willReturn(tossDto("DONE"));
		given(tossPaymentRepository.save(any(TossPayment.class)))
				.willThrow(new RuntimeException("저장 실패"));

		// when & then
		assertThatThrownBy(() -> paymentService.confirm(1L, new PaymentConfirmRequest("toss-order-1", "50000", "pk-1")))
				.isInstanceOf(RuntimeException.class);

		verify(tossPaymentClient).requestPaymentCancel(eq("pk-1"), anyString());
		assertThat(payment.getState()).isEqualTo(State.READY);
	}

	private PaymentTossDtoImpl tossDto(String status) {
		PaymentTossDtoImpl dto = new PaymentTossDtoImpl();
		dto.setPaymentKey("pk-1");
		dto.setOrderId("toss-order-1");
		dto.setTotalAmount(50000);
		dto.setMethod("카드");
		dto.setStatus(status);
		dto.setRequestedAt("2024-02-13T12:17:57+09:00");
		dto.setApprovedAt("2024-02-13T12:18:00+09:00");
		return dto;
	}
}
