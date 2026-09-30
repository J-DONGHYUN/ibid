package project.kjhjdh.ibid.product.presentation.dto;

import java.time.LocalDateTime;
import java.util.List;

import project.kjhjdh.ibid.product.application.ProductDetailResult;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;

public record ProductDetailResponse(
        Long productId,
        Long sellerId,
        String title,
        String description,
        int price,
        ProductStatus status,
        long viewCount,
        ProductCondition productCondition,
        List<String> imageUrls,
        LocalDateTime createdAt,
        List<String> tags
) {

    public static ProductDetailResponse from(ProductDetailResult result) {
        return new ProductDetailResponse(
                result.productId(),
                result.sellerId(),
                result.title(),
                result.description(),
                result.price(),
                result.status(),
                result.viewCount(),
                result.productCondition(),
                result.imageUrls(),
                result.createdAt(),
                result.tags()
        );
    }
}
