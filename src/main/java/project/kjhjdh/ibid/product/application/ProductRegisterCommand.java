package project.kjhjdh.ibid.product.application;

import project.kjhjdh.ibid.product.domain.ProductCondition;

public record ProductRegisterCommand(
        String title,
        String description,
        int price,
        ProductCondition productCondition
) {
}
