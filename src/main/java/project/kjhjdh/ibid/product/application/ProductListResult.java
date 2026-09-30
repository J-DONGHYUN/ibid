package project.kjhjdh.ibid.product.application;

import java.util.Map;

import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.product.domain.Product;

public record ProductListResult(
        Slice<Product> slice,
        Map<Long, String> thumbnails
) {
}
