package project.kjhjdh.ibid.trade.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.application.ProductListResult;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class TradeHistoryIntegrationTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;

    @Autowired
    private TradeHistoryService tradeHistoryService;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[TR-04] 내가 올린 상품을 최신순으로 조회하고 커서로 이어 받으면 겹침·빠짐이 없다")
    @Test
    void getSales_allAndCursor() {
        // given — 판매자의 상품 3개
        Long p1 = saveProduct(SELLER_ID);
        Long p2 = saveProduct(SELLER_ID);
        Long p3 = saveProduct(SELLER_ID);
        saveProduct(999L); // 다른 판매자 — 안 나와야 함

        // when — 첫 조회
        ProductListResult first = tradeHistoryService.getSales(SELLER_ID, null, null);

        // then — 내 상품만 최신순
        assertThat(ids(first)).containsExactly(p3, p2, p1);

        // when — p2 를 커서로 이어 받으면
        ProductListResult next = tradeHistoryService.getSales(SELLER_ID, null, p2);

        // then — p2 미만만
        assertThat(ids(next)).containsExactly(p1);
    }

    @DisplayName("[TR-04] 상태로 걸러 조회한다")
    @Test
    void getSales_statusFilter() {
        // given
        Long onSale = saveProduct(SELLER_ID);
        Long reserved = saveReserved(SELLER_ID, 31L);
        Long sold = saveSold(SELLER_ID, 32L);

        // when & then
        assertThat(ids(tradeHistoryService.getSales(SELLER_ID, ProductStatus.ON_SALE, null))).containsExactly(onSale);
        assertThat(ids(tradeHistoryService.getSales(SELLER_ID, ProductStatus.RESERVED, null))).containsExactly(reserved);
        assertThat(ids(tradeHistoryService.getSales(SELLER_ID, ProductStatus.SOLD, null))).containsExactly(sold);
    }

    @DisplayName("[TR-05] 내가 거래완료 상대로 지정된 상품을 조회한다")
    @Test
    void getPurchases() {
        // given — BUYER_ID 가 거래 상대인 거래완료 상품 2개, 다른 사람 상품 1개
        Long bought1 = saveSold(SELLER_ID, BUYER_ID);
        Long bought2 = saveSold(SELLER_ID, BUYER_ID);
        saveSold(SELLER_ID, 777L); // 다른 구매자

        // when
        ProductListResult result = tradeHistoryService.getPurchases(BUYER_ID, null);

        // then
        assertThat(ids(result)).containsExactly(bought2, bought1);
    }

    private java.util.List<Long> ids(ProductListResult result) {
        Slice<Product> slice = result.slice();
        return slice.getContent().stream().map(Product::getId).toList();
    }

    private Long saveProduct(Long sellerId) {
        return productRepository.save(Product.create(
                sellerId, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }

    private Long saveReserved(Long sellerId, Long buyerId) {
        Product product = Product.create(sellerId, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.reserve(buyerId);
        return productRepository.save(product).getId();
    }

    private Long saveSold(Long sellerId, Long buyerId) {
        Product product = Product.create(sellerId, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.complete(buyerId);
        return productRepository.save(product).getId();
    }
}
