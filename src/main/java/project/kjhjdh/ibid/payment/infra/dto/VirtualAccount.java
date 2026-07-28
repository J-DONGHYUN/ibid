package project.kjhjdh.ibid.payment.infra.dto;

public record VirtualAccount(
	String accountType,
	String accountNumber,
	String bankCode,
	String customerName,
	String dueDate,
	String refundStatus,
	boolean expired,
	String settlementStatus,
	RefundReceiveAccount refundReceiveAccount
) {
}
