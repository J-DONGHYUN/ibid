package project.kjhjdh.ibid.order.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.order.presentation.dto.MyOrdersResponse;
import project.kjhjdh.ibid.order.presentation.dto.MyTransactionsResponse;
import project.kjhjdh.ibid.order.presentation.dto.OrderDetailResponse;
import project.kjhjdh.ibid.order.presentation.dto.OrderSummaryResponse;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.product.presentation.dto.ProductSummaryResponse;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public PurchaseResult purchase(Long buyerId, PurchaseRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.validatePurchasable(buyerId, request.quantity());

        Order order = Order.create(
                product.getId(),
                buyerId,
                product.getSellerId(),
                request.quantity(),
                product.getPrice()
        );
        Order saved = orderRepository.save(order);
        return new PurchaseResult(saved.getId(), saved.getTotalPrice());
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
        order.cancel();
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
    public MyTransactionsResponse getMyTransactions(Long userId) {
        List<Order> purchaseOrders = orderRepository.findByBuyerIdOrderByIdDesc(userId);
        List<Order> saleOrders = orderRepository.findBySellerIdOrderByIdDesc(userId);

        Map<Long, Product> productsById = productRepository.findAllById(
                Stream.concat(purchaseOrders.stream(), saleOrders.stream())
                        .map(Order::getProductId)
                        .distinct()
                        .toList()
        ).stream().collect(Collectors.toMap(Product::getId, product -> product));

        List<OrderSummaryResponse> purchases = toSummaries(purchaseOrders, productsById);
        List<OrderSummaryResponse> sales = toSummaries(saleOrders, productsById);
        List<ProductSummaryResponse> listings = productRepository.findBySellerIdOrderByIdDesc(userId).stream()
                .map(ProductSummaryResponse::from)
                .toList();

        return new MyTransactionsResponse(purchases, sales, listings);
    }

    private List<OrderSummaryResponse> toSummaries(List<Order> orders, Map<Long, Product> productsById) {
        return orders.stream()
                .map(order -> OrderSummaryResponse.of(order, productsById.get(order.getProductId())))
                .toList();
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

}
