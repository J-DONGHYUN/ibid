package project.kjhjdh.ibid.payment.infra.dto;

public record EasyPay(
	String provider,
	Integer amount,
	Integer discountAmount
) {
}
