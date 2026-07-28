package project.kjhjdh.ibid.payment.infra.dto;

public record Cancels(
	Integer cancelAmount,
	String cancelReason,
	Integer taxFreeAmount,
	Integer taxExemptionAmount,
	Integer refundableAmount,
	Integer easyPayDiscountAmount,
	String canceledAt,
	String transactionKey,
	String receiptKey
) {
}
