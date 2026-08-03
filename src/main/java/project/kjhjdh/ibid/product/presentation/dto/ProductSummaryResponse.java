package project.kjhjdh.ibid.product.presentation.dto;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductStatus;

public record ProductSummaryResponse(
        Long productId,
        String title,
        int price,
        int stock,
        ProductStatus status,
        String thumbnailUrl
) {

    public static ProductSummaryResponse from(Product product, String thumbnailUrl) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                thumbnailUrl
        );
    }
}
