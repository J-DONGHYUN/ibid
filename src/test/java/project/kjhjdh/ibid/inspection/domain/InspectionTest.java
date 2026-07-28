package project.kjhjdh.ibid.inspection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InspectionTest {

    private static final Long ORDER_ID = 1L;
    private static final Long INSPECTOR_ID = 9L;

    @DisplayName("검수 통과 기록을 생성하면 통과 결과와 판정 내용이 저장된다")
    @Test
    void passed() {
        // when
        Inspection inspection = Inspection.passed(ORDER_ID, INSPECTOR_ID, "정품 확인");

        // then
        assertThat(inspection.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(inspection.getInspectorId()).isEqualTo(INSPECTOR_ID);
        assertThat(inspection.getResult()).isEqualTo(InspectionResult.PASSED);
        assertThat(inspection.getMemo()).isEqualTo("정품 확인");
        assertThat(inspection.isPassed()).isTrue();
    }

    @DisplayName("검수 불합격 기록을 생성하면 불합격 결과가 저장된다")
    @Test
    void failed() {
        // when
        Inspection inspection = Inspection.failed(ORDER_ID, INSPECTOR_ID, "가품 의심");

        // then
        assertThat(inspection.getResult()).isEqualTo(InspectionResult.FAILED);
        assertThat(inspection.isPassed()).isFalse();
    }
}
