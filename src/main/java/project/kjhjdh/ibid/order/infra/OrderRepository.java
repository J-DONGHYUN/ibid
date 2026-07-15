package project.kjhjdh.ibid.order.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
