package project.kjhjdh.ibid.order.presentation.dto;

import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.product.domain.Product;

public record OrderDetailResponse(
        Long orderId,
        Long productId,
        String productTitle,
        Long buyerId,
        Long sellerId,
        int quantity,
        int totalPrice,
        OrderStatus status
) {

    public static OrderDetailResponse of(Order order, Product product) {
        return new OrderDetailResponse(
                order.getId(),
                order.getProductId(),
                product == null ? null : product.getTitle(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus()
        );
    }
}
