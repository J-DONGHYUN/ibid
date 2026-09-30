package project.kjhjdh.ibid.product.application;

import project.kjhjdh.ibid.product.domain.Product;

public record ProductDetailResult(
        Product product,
        long viewCount
) {
}
