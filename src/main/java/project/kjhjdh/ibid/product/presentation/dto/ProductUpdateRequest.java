package project.kjhjdh.ibid.product.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import project.kjhjdh.ibid.product.domain.ProductCondition;

public record ProductUpdateRequest(
        @NotBlank(message = "상품명을 입력해주세요.") String title,
        @NotBlank(message = "상품 설명을 입력해주세요.") String description,
        @Positive(message = "판매가는 1원 이상이어야 합니다.") int price,
        @Positive(message = "재고 수량은 1개 이상이어야 합니다.") int stock,
        @NotNull(message = "상품 상태를 선택해주세요.") ProductCondition productCondition
) {
}
