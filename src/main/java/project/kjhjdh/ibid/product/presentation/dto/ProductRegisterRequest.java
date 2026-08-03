package project.kjhjdh.ibid.product.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import project.kjhjdh.ibid.product.domain.ProductCondition;

public record ProductRegisterRequest(
        @NotBlank(message = "상품 제목을 입력해주세요.")
        @Size(max = 100, message = "상품 제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "상품 설명을 입력해주세요.")
        @Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다.")
        String description,

        @NotNull(message = "판매가를 입력해주세요.")
        @Positive(message = "판매가는 1원 이상이어야 합니다.")
        Integer price,

        @NotNull(message = "재고 수량을 입력해주세요.")
        @Positive(message = "재고 수량은 1개 이상이어야 합니다.")
        Integer stock,

        @NotNull(message = "상품 상태를 선택해주세요.")
        ProductCondition productCondition
) {
}
