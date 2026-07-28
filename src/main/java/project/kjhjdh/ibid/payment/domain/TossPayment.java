package project.kjhjdh.ibid.payment.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "toss_payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TossPayment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String tossPaymentKey;

	@Column(nullable = false)
	private String tossOrderId;

	private Long paymentId;

	@Column(nullable = false)
	private long totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TossPaymentMethod tossPaymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TossPaymentStatus tossPaymentStatus;

	@Column(nullable = false)
	private LocalDateTime requestedAt;

	private LocalDateTime approvedAt;

	private TossPayment(
		String tossPaymentKey,
		String tossOrderId,
		Long paymentId,
		long totalAmount,
		TossPaymentMethod tossPaymentMethod,
		TossPaymentStatus tossPaymentStatus,
		LocalDateTime requestedAt,
		LocalDateTime approvedAt
	) {
		this.tossPaymentKey = tossPaymentKey;
		this.tossOrderId = tossOrderId;
		this.paymentId = paymentId;
		this.totalAmount = totalAmount;
		this.tossPaymentMethod = tossPaymentMethod;
		this.tossPaymentStatus = tossPaymentStatus;
		this.requestedAt = requestedAt;
		this.approvedAt = approvedAt;
	}

	public static TossPayment of(
		String tossPaymentKey,
		String tossOrderId,
		Long paymentId,
		long totalAmount,
		TossPaymentMethod tossPaymentMethod,
		TossPaymentStatus tossPaymentStatus,
		LocalDateTime requestedAt,
		LocalDateTime approvedAt
	) {
		return new TossPayment(
			tossPaymentKey,
			tossOrderId,
			paymentId,
			totalAmount,
			tossPaymentMethod,
			tossPaymentStatus,
			requestedAt,
			approvedAt);
	}
}
