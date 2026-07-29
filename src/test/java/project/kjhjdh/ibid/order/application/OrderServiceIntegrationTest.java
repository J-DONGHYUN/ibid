package project.kjhjdh.ibid.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class OrderServiceIntegrationTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @DisplayName("즉시구매하면 주문이 저장되고 재고는 그대로 유지된다")
    @Test
    void purchase() {
        // given
        Product product = onSaleProduct("나이키 후드", 89000, 3);

        // when
        PurchaseResult result = orderService.purchase(BUYER_ID, new PurchaseRequest(product.getId(), 2));

        // then
        assertThat(result.totalPrice()).isEqualTo(178000);
        Order order = orderRepository.findById(result.orderId()).orElseThrow();
        assertThat(order.getBuyerId()).isEqualTo(BUYER_ID);
        assertThat(order.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getTotalPrice()).isEqualTo(178000);

        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(found.getStock()).isEqualTo(3);
        assertThat(found.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @DisplayName("본인이 등록한 상품은 구매할 수 없다")
    @Test
    void purchase_selfTrade() {
        // given
        Product product = onSaleProduct("나이키 후드", 89000, 3);

        // when & then
        assertThatThrownBy(() -> orderService.purchase(SELLER_ID, new PurchaseRequest(product.getId(), 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SELF_TRADE_NOT_ALLOWED.getMessage());

        assertThat(orderRepository.count()).isZero();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    private Product onSaleProduct(String title, int price, int stock) {
        Product product = Product.create(SELLER_ID, title, "상태 좋음", price, stock);
        product.openForSale();
        return productRepository.save(product);
    }
}
