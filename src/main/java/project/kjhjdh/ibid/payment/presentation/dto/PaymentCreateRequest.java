package project.kjhjdh.ibid.payment.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentCreateRequest(
	@NotNull(message = "주문 ID는 필수입니다.") Long orderId
) {
}
