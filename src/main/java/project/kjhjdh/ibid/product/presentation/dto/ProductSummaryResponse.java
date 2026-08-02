package project.kjhjdh.ibid.product.presentation.dto;

import java.util.List;

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

    public static ProductSummaryResponse from(Product product) {
        List<String> images = product.getImageUrls();
        return new ProductSummaryResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                images.isEmpty() ? null : images.get(0)
        );
    }
}
