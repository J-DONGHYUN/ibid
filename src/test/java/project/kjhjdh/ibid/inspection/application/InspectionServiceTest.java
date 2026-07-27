package project.kjhjdh.ibid.inspection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import project.kjhjdh.ibid.inspection.domain.Inspection;
import project.kjhjdh.ibid.inspection.domain.InspectionPassed;
import project.kjhjdh.ibid.inspection.infra.InspectionRepository;
import project.kjhjdh.ibid.order.application.OrderService;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    private static final Long ORDER_ID = 100L;
    private static final Long INSPECTOR_ID = 9L;

    @Mock
    private OrderService orderService;

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InspectionService inspectionService;

    @Captor
    private ArgumentCaptor<Inspection> inspectionCaptor;

    @DisplayName("검수 수령은 해당 주문의 검수 시작으로 위임된다")
    @Test
    void receive() {
        // when
        inspectionService.receive(ORDER_ID);

        // then
        then(orderService).should().startInspection(ORDER_ID);
    }

    @DisplayName("검수 통과는 주문 완료 + 통과 기록 저장 + 통과 이벤트 발행을 수행한다")
    @Test
    void pass() {
        // when
        inspectionService.pass(INSPECTOR_ID, ORDER_ID, "정품 확인");

        // then
        then(orderService).should().complete(ORDER_ID);
        then(inspectionRepository).should().save(inspectionCaptor.capture());
        then(eventPublisher).should().publishEvent(new InspectionPassed(ORDER_ID));

        Inspection saved = inspectionCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(saved.getInspectorId()).isEqualTo(INSPECTOR_ID);
        assertThat(saved.isPassed()).isTrue();
        assertThat(saved.getMemo()).isEqualTo("정품 확인");
    }
}
