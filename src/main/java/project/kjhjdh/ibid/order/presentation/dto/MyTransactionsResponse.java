package project.kjhjdh.ibid.order.presentation.dto;

import java.util.List;

import project.kjhjdh.ibid.product.presentation.dto.ProductSummaryResponse;

public record MyTransactionsResponse(
        List<OrderSummaryResponse> purchases,
        List<OrderSummaryResponse> sales,
        List<ProductSummaryResponse> listings
) {
}
