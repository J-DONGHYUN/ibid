package project.kjhjdh.ibid.product.presentation.dto;

import java.util.List;

import project.kjhjdh.ibid.product.domain.Product;

public record ProductListResponse(
        List<ProductSummaryResponse> products
) {

    public static ProductListResponse from(List<Product> products) {
        return new ProductListResponse(
                products.stream()
                        .map(ProductSummaryResponse::from)
                        .toList()
        );
    }
}
