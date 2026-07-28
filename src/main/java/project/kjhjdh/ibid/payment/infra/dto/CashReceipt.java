package project.kjhjdh.ibid.payment.infra.dto;

public record CashReceipt(
	String type,
	String receiptKey,
	String issueNumber,
	String receiptUrl,
	Integer amount,
	Integer taxFreeAmount
) {
}
