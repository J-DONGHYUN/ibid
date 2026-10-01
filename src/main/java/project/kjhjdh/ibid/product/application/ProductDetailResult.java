package project.kjhjdh.ibid.product.application;

import java.time.LocalDateTime;
import java.util.List;

import project.kjhjdh.ibid.product.domain.DeviceSpec;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;

public record ProductDetailResult(
        Long productId,
        Long sellerId,
        String title,
        String description,
        int price,
        ProductStatus status,
        long viewCount,
        ProductCondition productCondition,
        DeviceSpec deviceSpec,
        List<String> imageUrls,
        LocalDateTime createdAt
) {

    public static ProductDetailResult from(Product product, long viewCount) {
        return new ProductDetailResult(
                product.getId(),
                product.getSellerId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                viewCount,
                product.getProductCondition(),
                product.getDeviceSpec(),
                product.imageUrls(),
                product.getCreatedAt()
        );
    }
}
