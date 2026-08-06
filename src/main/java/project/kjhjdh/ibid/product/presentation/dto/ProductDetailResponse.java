package project.kjhjdh.ibid.product.presentation.dto;

import java.time.LocalDateTime;
import java.util.List;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;

public record ProductDetailResponse(
        Long productId,
        Long sellerId,
        String title,
        String description,
        int price,
        int stock,
        ProductStatus status,
        ProductCondition productCondition,
        List<String> imageUrls,
        LocalDateTime createdAt,
        List<String> tags,
        int shippingFee
) {

    public static ProductDetailResponse from(Product product, List<String> imageUrls) {
        return new ProductDetailResponse(
                product.getId(),
                product.getSellerId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getProductCondition(),
                imageUrls,
                product.getCreatedAt(),
                List.copyOf(product.getTags()),
                product.getShippingFee()
        );
    }
}
