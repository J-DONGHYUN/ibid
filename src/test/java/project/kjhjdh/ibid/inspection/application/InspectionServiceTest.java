package project.kjhjdh.ibid.inspection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.inspection.domain.Inspection;
import project.kjhjdh.ibid.inspection.domain.InspectionFailed;
import project.kjhjdh.ibid.inspection.domain.InspectionPassed;
import project.kjhjdh.ibid.inspection.infra.InspectionRepository;
import project.kjhjdh.ibid.inspection.presentation.dto.InspectionQueueResponse;
import project.kjhjdh.ibid.order.application.OrderService;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    private static final Long ORDER_ID = 100L;
    private static final Long INSPECTOR_ID = 9L;

    @Mock
    private OrderService orderService;

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InspectionService inspectionService;

    @Captor
    private ArgumentCaptor<Inspection> inspectionCaptor;

    @DisplayName("검수 대기열은 발송/검수중 주문을 상품명과 함께 반환한다")
    @Test
    void getQueue() {
        // given
        Order order = Order.create(50L, 2L, 7L, 1, 89000);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_INSPECTOR);

        Product product = Product.create(7L, "나이키 에어맥스", "상품 설명", 89000, 1);
        ReflectionTestUtils.setField(product, "id", 50L);

        given(orderRepository.findByStatusInOrderByIdDesc(anyList())).willReturn(List.of(order));
        given(productRepository.findAllById(anyList())).willReturn(List.of(product));

        // when
        InspectionQueueResponse response = inspectionService.getQueue();

        // then
        assertThat(response.items()).hasSize(1);
        InspectionQueueResponse.Item item = response.items().get(0);
        assertThat(item.orderId()).isEqualTo(ORDER_ID);
        assertThat(item.productTitle()).isEqualTo("나이키 에어맥스");
        assertThat(item.price()).isEqualTo(89000);
        assertThat(item.sellerId()).isEqualTo(7L);
        assertThat(item.status()).isEqualTo(OrderStatus.SHIPPED_TO_INSPECTOR);
    }

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

    @DisplayName("검수 불합격은 주문 환불 + 불합격 기록 저장 + 불합격 이벤트 발행을 수행한다")
    @Test
    void fail() {
        // when
        inspectionService.fail(INSPECTOR_ID, ORDER_ID, "가품 의심");

        // then
        then(orderService).should().refund(ORDER_ID);
        then(inspectionRepository).should().save(inspectionCaptor.capture());
        then(eventPublisher).should().publishEvent(new InspectionFailed(ORDER_ID));

        Inspection saved = inspectionCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(saved.getInspectorId()).isEqualTo(INSPECTOR_ID);
        assertThat(saved.isPassed()).isFalse();
        assertThat(saved.getMemo()).isEqualTo("가품 의심");
    }
}
