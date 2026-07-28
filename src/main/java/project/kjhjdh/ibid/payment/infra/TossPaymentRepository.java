package project.kjhjdh.ibid.payment.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.payment.domain.TossPayment;

public interface TossPaymentRepository extends JpaRepository<TossPayment, Long> {
}
