package project.kjhjdh.ibid.product.presentation.dto;

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
        ProductCondition condition,
        List<String> imageUrls
) {

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getSellerId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getCondition(),
                List.copyOf(product.getImageUrls())
        );
    }
}
