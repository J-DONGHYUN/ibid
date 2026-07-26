package project.kjhjdh.ibid.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Long ORDER_ID = 100L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @DisplayName("즉시구매에 성공하면 재고가 차감되고 주문 id를 반환한다")
    @Test
    void purchase() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();
        given(productRepository.findByIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(product));
        Order savedOrder = mock(Order.class);
        given(savedOrder.getId()).willReturn(100L);
        given(savedOrder.getTotalPrice()).willReturn(178000);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        PurchaseResult result = orderService.purchase(BUYER_ID, new PurchaseRequest(PRODUCT_ID, 2));

        // then
        assertThat(result.orderId()).isEqualTo(100L);
        assertThat(result.totalPrice()).isEqualTo(178000);
        assertThat(product.getStock()).isEqualTo(1);
        verify(orderRepository).save(any(Order.class));
    }

    @DisplayName("존재하지 않는 상품을 구매하면 실패한다")
    @Test
    void purchase_productNotFound() {
        // given
        given(productRepository.findByIdForUpdate(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.purchase(BUYER_ID, new PurchaseRequest(PRODUCT_ID, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @DisplayName("본인이 등록한 상품은 구매할 수 없다")
    @Test
    void purchase_selfTrade() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findByIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.purchase(SELLER_ID, new PurchaseRequest(PRODUCT_ID, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SELF_TRADE_NOT_ALLOWED.getMessage());
        assertThat(product.getStock()).isEqualTo(3);
        verify(orderRepository, never()).save(any());
    }

    @DisplayName("재고보다 많은 수량을 구매하면 실패하고 주문이 생성되지 않는다")
    @Test
    void purchase_insufficientStock() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        product.openForSale();
        given(productRepository.findByIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.purchase(BUYER_ID, new PurchaseRequest(PRODUCT_ID, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());
        assertThat(product.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any());
    }

    @DisplayName("결제가 확정되면 주문 상태가 PAID가 된다")
    @Test
    void confirmPaid() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

        // when
        orderService.confirmPaid(ORDER_ID);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @DisplayName("존재하지 않는 주문은 결제 확정할 수 없다")
    @Test
    void confirmPaid_orderNotFound() {
        // given
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.confirmPaid(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @DisplayName("판매자가 결제된 주문을 발송하면 검수업체 발송 상태가 된다")
    @Test
    void ship() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        order.confirmPaid();
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

        // when
        orderService.ship(SELLER_ID, ORDER_ID);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED_TO_INSPECTOR);
    }

    @DisplayName("판매자가 아니면 발송할 수 없다")
    @Test
    void ship_notSeller() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        order.confirmPaid();
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.ship(BUYER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @DisplayName("존재하지 않는 주문은 발송할 수 없다")
    @Test
    void ship_orderNotFound() {
        // given
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.ship(SELLER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @DisplayName("주문을 취소하면 재고가 복원되고 상태가 CANCELED가 된다")
    @Test
    void cancel() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();
        product.decreaseStock(2);
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(productRepository.findByIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        orderService.cancel(ORDER_ID);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(product.getStock()).isEqualTo(3);
    }

    @DisplayName("존재하지 않는 주문은 취소할 수 없다")
    @Test
    void cancel_orderNotFound() {
        // given
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.cancel(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
        verify(productRepository, never()).findByIdForUpdate(any());
    }
}
