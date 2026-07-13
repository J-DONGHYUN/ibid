package project.kjhjdh.ibid.product.presentation.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.product.domain.Product;

public record ProductListResponse(
        List<ProductSummaryResponse> products,
        Long nextCursor,
        boolean hasNext
) {

    public static ProductListResponse of(Slice<Product> slice) {
        List<Product> content = slice.getContent();
        Long nextCursor = slice.hasNext() && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;
        return new ProductListResponse(
                content.stream()
                        .map(ProductSummaryResponse::from)
                        .toList(),
                nextCursor,
                slice.hasNext()
        );
    }
}
