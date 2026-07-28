package project.kjhjdh.ibid.payment.infra.dto;

public record Card(
	Integer amount,
	String issuerCode,
	String acquirerCode,
	String number,
	Integer installmentPlanMonths,
	String approveNo,
	boolean useCardPoint,
	String cardType,
	String ownerType,
	String acquireStatus,
	String isInterestFree,
	String interestPayer
) {
}
