package project.kjhjdh.ibid.product.presentation.dto;

import project.kjhjdh.ibid.product.domain.DeviceCategory;
import project.kjhjdh.ibid.product.domain.DeviceSpec;

public record DeviceSpecResponse(
        DeviceCategory category,
        String modelName,
        int batteryHealth,
        String components,
        String defects
) {

    public static DeviceSpecResponse from(DeviceSpec deviceSpec) {
        return new DeviceSpecResponse(
                deviceSpec.category(),
                deviceSpec.modelName(),
                deviceSpec.batteryHealth(),
                deviceSpec.components(),
                deviceSpec.defects()
        );
    }
}
