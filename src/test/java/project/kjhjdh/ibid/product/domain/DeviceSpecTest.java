package project.kjhjdh.ibid.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

class DeviceSpecTest {

    @DisplayName("[PD-01] 전자기기 정보를 갖춰 값 객체를 만든다")
    @Test
    void create() {
        // when
        DeviceSpec spec = new DeviceSpec(DeviceCategory.SMARTPHONE, "iPhone 13", 87, "본체, 케이블", "액정 잔기스");

        // then
        assertThat(spec.category()).isEqualTo(DeviceCategory.SMARTPHONE);
        assertThat(spec.batteryHealth()).isEqualTo(87);
    }

    @DisplayName("[PD-01] 배터리 성능 경계값 0·100 은 허용된다")
    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void create_batteryBoundary(int batteryHealth) {
        // when & then
        assertThatCode(() -> new DeviceSpec(DeviceCategory.LAPTOP, "MacBook", batteryHealth, "본체", null))
                .doesNotThrowAnyException();
    }

    @DisplayName("[PD-01] 배터리 성능이 0~100 밖이면 실패한다")
    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void create_batteryOutOfRange(int batteryHealth) {
        // when & then
        assertThatThrownBy(() -> new DeviceSpec(DeviceCategory.LAPTOP, "MacBook", batteryHealth, "본체", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_BATTERY_HEALTH.getMessage());
    }

    @DisplayName("[PD-01] 카테고리가 없으면 실패한다")
    @Test
    void create_nullCategory() {
        // when & then
        assertThatThrownBy(() -> new DeviceSpec(null, "iPhone 13", 90, "본체", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_DEVICE_CATEGORY.getMessage());
    }

    @DisplayName("[PD-01] 모델명이 비면 실패한다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_blankModelName(String modelName) {
        // when & then
        assertThatThrownBy(() -> new DeviceSpec(DeviceCategory.SMARTPHONE, modelName, 90, "본체", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_DEVICE_MODEL.getMessage());
    }

    @DisplayName("[PD-01] 구성품이 비면 실패한다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_blankComponents(String components) {
        // when & then
        assertThatThrownBy(() -> new DeviceSpec(DeviceCategory.SMARTPHONE, "iPhone 13", 90, components, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_DEVICE_COMPONENTS.getMessage());
    }

    @DisplayName("[PD-01] 하자는 비워도 만들 수 있다")
    @Test
    void create_blankDefectsAllowed() {
        // when & then
        assertThatCode(() -> new DeviceSpec(DeviceCategory.SMARTPHONE, "iPhone 13", 90, "본체", null))
                .doesNotThrowAnyException();
    }
}
