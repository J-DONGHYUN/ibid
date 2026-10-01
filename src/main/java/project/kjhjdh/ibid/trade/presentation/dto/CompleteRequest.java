package project.kjhjdh.ibid.trade.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record CompleteRequest(
        @NotNull(message = "거래완료할 상대를 선택해주세요.")
        Long buyerId
) {
}
