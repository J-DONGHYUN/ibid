package project.kjhjdh.ibid.order.presentation.dto;

import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.product.domain.Product;

public record OrderSummaryResponse(
        Long orderId,
        Long productId,
        String productTitle,
        int quantity,
        int totalPrice,
        OrderStatus status
) {

    public static OrderSummaryResponse of(Order order, Product product) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getProductId(),
                product == null ? null : product.getTitle(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus()
        );
    }
}
