package project.kjhjdh.ibid.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import project.kjhjdh.ibid.payment.infra.dto.TossConfirmRequest;

public record PaymentConfirmRequest(
	@NotBlank String orderId,
	@NotBlank String amount,
	@NotBlank String paymentKey
) {

	public TossConfirmRequest toTossConfirmRequest() {
		return new TossConfirmRequest(orderId, amount, paymentKey);
	}
}
