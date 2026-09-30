package project.kjhjdh.ibid.product.application;

import project.kjhjdh.ibid.product.domain.Product;

public record ProductWithThumbnail(
        Product product,
        String thumbnailUrl
) {
}
