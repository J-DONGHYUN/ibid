package project.kjhjdh.ibid.payment.infra;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.exception.GlobalException;
import project.kjhjdh.ibid.payment.infra.dto.PaymentTossDtoImpl;
import project.kjhjdh.ibid.payment.infra.dto.TossConfirmRequest;

@Component
@RequiredArgsConstructor
public class TossPaymentClient {

	private final RestClient tossPaymentRestClient;

	public PaymentTossDtoImpl requestConfirm(TossConfirmRequest confirmPaymentRequest) {
		return tossPaymentRestClient.post()
			.uri("/confirm")
			.body(confirmPaymentRequest)
			.retrieve()
			.onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
				throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
			})
			.body(PaymentTossDtoImpl.class);
	}

	public PaymentTossDtoImpl requestPaymentCancel(String paymentKey, String cancelReason) {
		return tossPaymentRestClient.post()
			.uri("/{paymentKey}/cancel", paymentKey)
			.body(Map.of("cancelReason", cancelReason))
			.retrieve()
			.onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
				throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
			})
			.body(PaymentTossDtoImpl.class);
	}
}
