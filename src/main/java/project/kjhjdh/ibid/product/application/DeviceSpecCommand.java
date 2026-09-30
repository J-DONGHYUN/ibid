package project.kjhjdh.ibid.product.application;

import project.kjhjdh.ibid.product.domain.DeviceCategory;

public record DeviceSpecCommand(
        DeviceCategory category,
        String modelName,
        int batteryHealth,
        String components,
        String defects
) {
}
