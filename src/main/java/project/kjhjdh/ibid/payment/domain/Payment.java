package project.kjhjdh.ibid.payment.domain;

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

@Getter
@Entity
@Table(name = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long orderId;

	@Column(nullable = false)
	private long totalAmount;

	private String paymentKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private State state;

	private Payment(Long orderId, long totalAmount, String paymentKey, State state) {
		this.orderId = orderId;
		this.totalAmount = totalAmount;
		this.paymentKey = paymentKey;
		this.state = state;
	}

	public static Payment ready(Long orderId, long totalAmount) {
		return new Payment(orderId, totalAmount, null, State.READY);
	}

	public void confirm(String paymentKey) {
		this.paymentKey = paymentKey;
		this.state = State.CONFIRMED;
	}
}
