package project.kjhjdh.ibid.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.payment.domain.TossPayment;
import project.kjhjdh.ibid.payment.infra.TossPaymentClient;
import project.kjhjdh.ibid.payment.infra.TossPaymentRepository;
import project.kjhjdh.ibid.payment.infra.dto.PaymentTossDtoImpl;
import project.kjhjdh.ibid.payment.infra.dto.TossConfirmRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;

@ExtendWith(MockitoExtension.class)
class PaymentTossConfirmHandlerTest {

	@Mock
	private TossPaymentClient tossPaymentClient;

	@Mock
	private TossPaymentRepository tossPaymentRepository;

	@InjectMocks
	private PaymentTossConfirmHandler paymentTossConfirmHandler;

	@DisplayName("토스 승인에 성공하면 TossPayment를 저장하고 승인 결과를 반환한다")
	@Test
	void confirm() {
		// given
		TossConfirmRequest request = new TossConfirmRequest("toss-order-1", "50000", "pk-1");
		given(tossPaymentClient.requestConfirm(request)).willReturn(tossDto("DONE"));

		// when
		PaymentConfirmResponse result = paymentTossConfirmHandler.confirm(1L, request);

		// then
		assertThat(result.getPaymentKey()).isEqualTo("pk-1");
		verify(tossPaymentRepository).save(any(TossPayment.class));
		verify(tossPaymentClient, never()).requestPaymentCancel(anyString(), anyString());
	}

	@DisplayName("토스 승인 자체가 실패하면 취소 요청 없이 예외를 전파한다")
	@Test
	void confirm_tossConfirmFailed() {
		// given
		TossConfirmRequest request = new TossConfirmRequest("toss-order-1", "50000", "pk-1");
		given(tossPaymentClient.requestConfirm(request))
				.willThrow(new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED));

		// when & then
		assertThatThrownBy(() -> paymentTossConfirmHandler.confirm(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.PAYMENT_CONFIRM_FAILED.getMessage());

		verify(tossPaymentRepository, never()).save(any());
		verify(tossPaymentClient, never()).requestPaymentCancel(anyString(), anyString());
	}

	@DisplayName("승인 성공 후 저장이 실패하면 토스에 취소를 요청하고 예외를 전파한다")
	@Test
	void confirm_saveFailed() {
		// given
		TossConfirmRequest request = new TossConfirmRequest("toss-order-1", "50000", "pk-1");
		given(tossPaymentClient.requestConfirm(request)).willReturn(tossDto("DONE"));
		given(tossPaymentRepository.save(any(TossPayment.class)))
				.willThrow(new RuntimeException("저장 실패"));

		// when & then
		assertThatThrownBy(() -> paymentTossConfirmHandler.confirm(1L, request))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("저장 실패");

		verify(tossPaymentClient).requestPaymentCancel(eq("pk-1"), anyString());
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
