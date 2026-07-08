package project.kjhjdh.ibid.order.application;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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

    private static final int MAX_OPTIMISTIC_RETRY = 100;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Long purchase(Long buyerId, PurchaseRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return settle(product, buyerId, request.quantity());
    }

    @Transactional
    public Long purchasePessimistic(Long buyerId, PurchaseRequest request) {
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return settle(product, buyerId, request.quantity());
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = MAX_OPTIMISTIC_RETRY,
            backoff = @Backoff(delay = 0)
    )
    @Transactional
    public Long purchaseOptimistic(Long buyerId, PurchaseRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return settle(product, buyerId, request.quantity());
    }

    private Long settle(Product product, Long buyerId, int quantity) {
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
        return orderRepository.save(order).getId();
    }
}
