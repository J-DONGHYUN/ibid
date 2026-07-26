package project.kjhjdh.ibid.order.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public PurchaseResult purchase(Long buyerId, PurchaseRequest request) {
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return settle(product, buyerId, request.quantity());
    }

    @Transactional
    public void confirmPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.confirmPaid();
    }

    @Transactional
    public void ship(Long sellerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.isSeller(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        order.ship();
    }

    @Transactional
    public void cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Product product = productRepository.findByIdForUpdate(order.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        order.cancel();
        product.restoreStock(order.getQuantity());
    }

    private PurchaseResult settle(Product product, Long buyerId, int quantity) {
        if (product.isOwnedBy(buyerId)) {
            throw new BusinessException(ErrorCode.SELF_TRADE_NOT_ALLOWED);
        }

        product.decreaseStock(quantity);

        Order order = Order.create(
                product.getId(),
                buyerId,
                product.getSellerId(),
                quantity,
                product.getPrice()
        );
        Order saved = orderRepository.save(order);
        return new PurchaseResult(saved.getId(), saved.getTotalPrice());
    }
}
