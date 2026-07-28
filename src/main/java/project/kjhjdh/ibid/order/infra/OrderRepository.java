package project.kjhjdh.ibid.order.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByIdDesc(Long buyerId);

    List<Order> findBySellerIdOrderByIdDesc(Long sellerId);
}
