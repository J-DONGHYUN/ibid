package project.kjhjdh.ibid.inspection.application;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.order.application.OrderService;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    private static final Long ORDER_ID = 100L;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private InspectionService inspectionService;

    @DisplayName("검수 수령은 해당 주문의 검수 시작으로 위임된다")
    @Test
    void receive() {
        // when
        inspectionService.receive(ORDER_ID);

        // then
        then(orderService).should().startInspection(ORDER_ID);
    }
}
