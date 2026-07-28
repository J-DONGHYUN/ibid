package project.kjhjdh.ibid.payment.infra.dto;

public record Transfer(
	String bankCode,
	String settlementStatus
) {
}
