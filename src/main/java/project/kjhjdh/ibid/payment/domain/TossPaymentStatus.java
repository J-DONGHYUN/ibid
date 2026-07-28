package project.kjhjdh.ibid.payment.domain;

public enum TossPaymentStatus {

	READY,
	IN_PROGRESS,
	WAITING_FOR_DEPOSIT,
	DONE,
	CANCELED,
	PARTIAL_CANCELED,
	ABORTED,
	EXPIRED;

    public static TossPaymentStatus from() {
        return null;
    }
}
