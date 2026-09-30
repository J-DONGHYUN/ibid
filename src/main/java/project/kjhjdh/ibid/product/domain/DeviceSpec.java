package project.kjhjdh.ibid.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@Embeddable
public record DeviceSpec(

        @Enumerated(EnumType.STRING)
        @Column(name = "device_category", nullable = false)
        DeviceCategory category,

        @Column(name = "model_name", nullable = false)
        String modelName,

        @Column(name = "battery_health", nullable = false)
        int batteryHealth,

        @Column(name = "components", nullable = false)
        String components,

        @Column(name = "defects")
        String defects
) {

    private static final int MIN_BATTERY_HEALTH = 0;
    private static final int MAX_BATTERY_HEALTH = 100;

    public DeviceSpec {
        if (category == null) {
            throw new BusinessException(ErrorCode.INVALID_DEVICE_CATEGORY);
        }
        if (modelName == null || modelName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_DEVICE_MODEL);
        }
        if (batteryHealth < MIN_BATTERY_HEALTH || batteryHealth > MAX_BATTERY_HEALTH) {
            throw new BusinessException(ErrorCode.INVALID_BATTERY_HEALTH);
        }
        if (components == null || components.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_DEVICE_COMPONENTS);
        }
    }
}
