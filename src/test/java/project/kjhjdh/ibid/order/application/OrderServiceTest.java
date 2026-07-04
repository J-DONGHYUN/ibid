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
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;

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
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        Order savedOrder = mock(Order.class);
        given(savedOrder.getId()).willReturn(100L);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        Long orderId = orderService.purchase(BUYER_ID, new PurchaseRequest(PRODUCT_ID, 2));

        // then
        assertThat(orderId).isEqualTo(100L);
        assertThat(product.getStock()).isEqualTo(1);
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

    @DisplayName("재고보다 많은 수량을 구매하면 실패하고 주문이 생성되지 않는다")
    @Test
    void purchase_insufficientStock() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.purchase(BUYER_ID, new PurchaseRequest(PRODUCT_ID, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());
        assertThat(product.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any());
    }
}
