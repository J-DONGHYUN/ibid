package project.kjhjdh.ibid.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.inspection.application.InspectionService;
import project.kjhjdh.ibid.inspection.domain.Inspection;
import project.kjhjdh.ibid.inspection.infra.InspectionRepository;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class TradeScenarioIntegrationTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Long INSPECTOR_ID = 9L;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InspectionRepository inspectionRepository;

    @DisplayName("구매→결제확정→발송→검수수령→통과 전 과정을 거치면 주문이 완료되고 통과 기록이 남는다")
    @Test
    void fullCycle_pass() {
        // given
        Long productId = productRepository.save(onSaleProduct(89000, 3)).getId();
        Long orderId = orderService.purchase(BUYER_ID, new PurchaseRequest(productId, 1)).orderId();

        // when
        orderService.confirmPaid(orderId);
        orderService.ship(SELLER_ID, orderId);
        inspectionService.receive(orderId);
        inspectionService.pass(INSPECTOR_ID, orderId, "정품 확인");

        // then
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStock()).isEqualTo(2);

        Inspection inspection = inspectionRepository.findByOrderId(orderId).orElseThrow();
        assertThat(inspection.isPassed()).isTrue();
        assertThat(inspection.getInspectorId()).isEqualTo(INSPECTOR_ID);
    }

    @DisplayName("검수 불합격이면 주문이 환불되고 재고가 복원되며 불합격 기록이 남는다")
    @Test
    void fullCycle_fail() {
        // given
        Long productId = productRepository.save(onSaleProduct(89000, 3)).getId();
        Long orderId = orderService.purchase(BUYER_ID, new PurchaseRequest(productId, 1)).orderId();
        orderService.confirmPaid(orderId);
        orderService.ship(SELLER_ID, orderId);
        inspectionService.receive(orderId);

        // when
        inspectionService.fail(INSPECTOR_ID, orderId, "가품 의심");

        // then
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);

        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStock()).isEqualTo(3);

        Inspection inspection = inspectionRepository.findByOrderId(orderId).orElseThrow();
        assertThat(inspection.isPassed()).isFalse();
    }

    @DisplayName("결제 확정 전에는 발송할 수 없다")
    @Test
    void cannotShipBeforePaid() {
        // given
        Long productId = productRepository.save(onSaleProduct(89000, 3)).getId();
        Long orderId = orderService.purchase(BUYER_ID, new PurchaseRequest(productId, 1)).orderId();

        // when & then
        assertThatThrownBy(() -> orderService.ship(SELLER_ID, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_SHIPPABLE.getMessage());
    }

    private Product onSaleProduct(int price, int stock) {
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", price, stock);
        product.openForSale();
        return product;
    }
}
