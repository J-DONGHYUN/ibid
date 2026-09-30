package project.kjhjdh.ibid.product.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import project.kjhjdh.ibid.product.domain.DeviceCategory;

public record DeviceSpecRequest(
        @NotNull(message = "전자기기 카테고리를 선택해주세요.")
        DeviceCategory category,

        @NotBlank(message = "모델명을 입력해주세요.")
        String modelName,

        @NotNull(message = "배터리 성능을 입력해주세요.")
        @Min(value = 0, message = "배터리 성능은 0에서 100 사이여야 합니다.")
        @Max(value = 100, message = "배터리 성능은 0에서 100 사이여야 합니다.")
        Integer batteryHealth,

        @NotBlank(message = "구성품을 입력해주세요.")
        String components,

        String defects
) {
}
