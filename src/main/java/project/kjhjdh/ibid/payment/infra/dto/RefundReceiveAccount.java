package project.kjhjdh.ibid.payment.infra.dto;

public record RefundReceiveAccount(
	String bankCode,
	String accountNumber,
	String holderName
) {
}
