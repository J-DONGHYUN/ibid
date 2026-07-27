package project.kjhjdh.ibid.order.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.order.presentation.dto.MyOrdersResponse;
import project.kjhjdh.ibid.order.presentation.dto.OrderDetailResponse;
import project.kjhjdh.ibid.order.presentation.dto.OrderSummaryResponse;
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
    public void startInspection(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.startInspection();
    }

    @Transactional
    public void complete(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.complete();
    }

    @Transactional
    public void refund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Product product = productRepository.findByIdForUpdate(order.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        order.refund();
        product.restoreStock(order.getQuantity());
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

    @Transactional(readOnly = true)
    public MyOrdersResponse getMyOrders(Long userId, String role) {
        List<Order> orders = (OrderRole.from(role) == OrderRole.SELLER)
                ? orderRepository.findBySellerIdOrderByIdDesc(userId)
                : orderRepository.findByBuyerIdOrderByIdDesc(userId);

        Map<Long, Product> productsById = productRepository.findAllById(
                orders.stream().map(Order::getProductId).distinct().toList()
        ).stream().collect(Collectors.toMap(Product::getId, product -> product));

        List<OrderSummaryResponse> items = orders.stream()
                .map(order -> OrderSummaryResponse.of(order, productsById.get(order.getProductId())))
                .toList();
        return MyOrdersResponse.of(items);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.isBuyer(userId) && !order.isSeller(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Product product = productRepository.findById(order.getProductId()).orElse(null);
        return OrderDetailResponse.of(order, product);
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
