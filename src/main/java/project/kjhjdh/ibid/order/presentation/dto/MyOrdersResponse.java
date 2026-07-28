package project.kjhjdh.ibid.order.presentation.dto;

import java.util.List;

public record MyOrdersResponse(List<OrderSummaryResponse> orders) {

    public static MyOrdersResponse of(List<OrderSummaryResponse> orders) {
        return new MyOrdersResponse(orders);
    }
}
