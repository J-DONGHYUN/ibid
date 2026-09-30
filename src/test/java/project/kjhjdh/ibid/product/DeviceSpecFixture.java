package project.kjhjdh.ibid.product;

import project.kjhjdh.ibid.product.domain.DeviceCategory;
import project.kjhjdh.ibid.product.domain.DeviceSpec;

public final class DeviceSpecFixture {

    private DeviceSpecFixture() {
    }

    public static DeviceSpec sample() {
        return new DeviceSpec(DeviceCategory.SMARTPHONE, "iPhone 13", 90, "본체, 충전기", null);
    }
}
