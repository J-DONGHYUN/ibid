package project.kjhjdh.ibid.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
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
import project.kjhjdh.ibid.order.presentation.dto.MyOrdersResponse;
import project.kjhjdh.ibid.order.presentation.dto.OrderDetailResponse;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Long ORDER_ID = 100L;
    private static final Long STRANGER_ID = 999L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @DisplayName("즉시구매에 성공하면 주문이 생성되고 주문 id를 반환한다")
    @Test
    void purchase() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        Order savedOrder = mock(Order.class);
        given(savedOrder.getId()).willReturn(100L);
        given(savedOrder.getTotalPrice()).willReturn(178000);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        PurchaseResult result = orderService.purchase(BUYER_ID, new PurchaseRequest(PRODUCT_ID, 2));

        // then
        assertThat(result.orderId()).isEqualTo(100L);
        assertThat(result.totalPrice()).isEqualTo(178000);
        verify(orderRepository).save(any(Order.class));
    }

    @DisplayName("존재하지 않는 상품을 구매하면 실패한다")
    @Test
    void purchase_productNotFound() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.purchase(BUYER_ID, new PurchaseRequest(PRODUCT_ID, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @DisplayName("재고보다 많은 수량을 구매하면 실패하고 주문이 생성되지 않는다")
    @Test
    void purchase_insufficientStock() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        product.openForSale();
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.purchase(BUYER_ID, new PurchaseRequest(PRODUCT_ID, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());
        assertThat(product.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any());
    }

    @DisplayName("본인이 등록한 상품은 구매할 수 없다")
    @Test
    void purchase_selfTrade() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.purchase(SELLER_ID, new PurchaseRequest(PRODUCT_ID, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SELF_TRADE_NOT_ALLOWED.getMessage());
        assertThat(product.getStock()).isEqualTo(3);
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

    @DisplayName("검수를 시작하면 주문이 검수중 상태가 된다")
    @Test
    void startInspection() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        order.confirmPaid();
        order.ship();
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

        // when
        orderService.startInspection(ORDER_ID);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.UNDER_INSPECTION);
    }

    @DisplayName("존재하지 않는 주문은 검수를 시작할 수 없다")
    @Test
    void startInspection_orderNotFound() {
        // given
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.startInspection(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @DisplayName("검수를 완료하면 주문이 거래 완료 상태가 된다")
    @Test
    void complete() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        order.confirmPaid();
        order.ship();
        order.startInspection();
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

        // when
        orderService.complete(ORDER_ID);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @DisplayName("존재하지 않는 주문은 완료할 수 없다")
    @Test
    void complete_orderNotFound() {
        // given
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.complete(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @DisplayName("검수 불합격으로 환불하면 주문이 환불 상태가 되고 재고가 복원된다")
    @Test
    void refund() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();
        product.decreaseStock(2);
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        order.confirmPaid();
        order.ship();
        order.startInspection();
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(productRepository.findByIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        orderService.refund(ORDER_ID);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(product.getStock()).isEqualTo(3);
    }

    @DisplayName("존재하지 않는 주문은 환불할 수 없다")
    @Test
    void refund_orderNotFound() {
        // given
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.refund(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @DisplayName("주문을 취소하면 상태가 CANCELED가 되고 재고는 건드리지 않는다 (차감 전 상태)")
    @Test
    void cancel() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

        // when
        orderService.cancel(ORDER_ID);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(productRepository, never()).findByIdForUpdate(any());
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

    @DisplayName("구매자 관점 내 거래 목록은 상품명이 채워져 반환된다")
    @Test
    void getMyOrders() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        Product product = mock(Product.class);
        given(product.getId()).willReturn(PRODUCT_ID);
        given(product.getTitle()).willReturn("나이키 후드");
        given(orderRepository.findByBuyerIdOrderByIdDesc(BUYER_ID)).willReturn(List.of(order));
        given(productRepository.findAllById(List.of(PRODUCT_ID))).willReturn(List.of(product));

        // when
        MyOrdersResponse response = orderService.getMyOrders(BUYER_ID, "buyer");

        // then
        assertThat(response.orders()).hasSize(1);
        assertThat(response.orders().get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.orders().get(0).productTitle()).isEqualTo("나이키 후드");
        assertThat(response.orders().get(0).totalPrice()).isEqualTo(178000);
    }

    @DisplayName("role 값이 올바르지 않으면 목록 조회에 실패한다")
    @Test
    void getMyOrders_invalidRole() {
        // when & then
        assertThatThrownBy(() -> orderService.getMyOrders(BUYER_ID, "invalid"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT.getMessage());
    }

    @DisplayName("거래 당사자는 주문 상세를 조회할 수 있다")
    @Test
    void getMyOrder() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        OrderDetailResponse response = orderService.getMyOrder(BUYER_ID, ORDER_ID);

        // then
        assertThat(response.buyerId()).isEqualTo(BUYER_ID);
        assertThat(response.sellerId()).isEqualTo(SELLER_ID);
        assertThat(response.productTitle()).isEqualTo("나이키 후드");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
    }

    @DisplayName("거래 당사자가 아니면 주문 상세를 조회할 수 없다")
    @Test
    void getMyOrder_notParty() {
        // given
        Order order = Order.create(PRODUCT_ID, BUYER_ID, SELLER_ID, 2, 89000);
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.getMyOrder(STRANGER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("존재하지 않는 주문 상세는 조회할 수 없다")
    @Test
    void getMyOrder_orderNotFound() {
        // given
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.getMyOrder(BUYER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }
}
