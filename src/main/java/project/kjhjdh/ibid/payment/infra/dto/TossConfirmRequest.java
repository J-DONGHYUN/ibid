package project.kjhjdh.ibid.payment.infra.dto;

public record TossConfirmRequest(
	String orderId,
	String amount,
	String paymentKey
) {
}
