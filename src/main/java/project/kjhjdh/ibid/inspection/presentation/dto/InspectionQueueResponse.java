package project.kjhjdh.ibid.inspection.presentation.dto;

import java.util.List;

import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.product.domain.Product;

public record InspectionQueueResponse(List<Item> items) {

    public record Item(
            Long orderId,
            String productTitle,
            int price,
            Long sellerId,
            OrderStatus status
    ) {

        public static Item of(Order order, Product product) {
            return new Item(
                    order.getId(),
                    product == null ? null : product.getTitle(),
                    order.getTotalPrice(),
                    order.getSellerId(),
                    order.getStatus()
            );
        }
    }
}
