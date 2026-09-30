package project.kjhjdh.ibid.product.application;

import java.util.List;

import project.kjhjdh.ibid.product.domain.ProductCondition;

public record ProductRegisterCommand(
        String title,
        String description,
        int price,
        ProductCondition productCondition,
        List<String> tags
) {
}
