package project.kjhjdh.ibid.trade.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record ReserveRequest(
        @NotNull(message = "예약할 상대를 선택해주세요.")
        Long buyerId
) {
}
