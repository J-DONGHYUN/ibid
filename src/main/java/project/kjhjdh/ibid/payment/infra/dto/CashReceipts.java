package project.kjhjdh.ibid.payment.infra.dto;

public record CashReceipts(
	String receiptKey,
	String orderId,
	String orderName,
	String type,
	String issueNumber,
	String receiptUrl,
	String businessNumber,
	String transactionType,
	Integer amount,
	Integer taxFreeAmount,
	String issueStatus,
	Failure failure,
	String customerIdentityNumber,
	String requestedAt
) {
}
