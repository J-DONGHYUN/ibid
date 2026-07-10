package project.kjhjdh.ibid.product.presentation.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import project.kjhjdh.ibid.product.domain.Product;

public record ProductListResponse(
        List<ProductSummaryResponse> products,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ProductListResponse from(Page<Product> products) {
        return new ProductListResponse(
                products.getContent().stream()
                        .map(ProductSummaryResponse::from)
                        .toList(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.hasNext()
        );
    }
}
