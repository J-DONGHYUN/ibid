package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.product.infra.ProductViewRedisRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductViewCountFlusherTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;

    @Autowired
    private ProductViewCountFlusher productViewCountFlusher;

    @Autowired
    private ProductViewCounter productViewCounter;

    @Autowired
    private ProductViewRedisRepository productViewRedisRepository;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("미반영 조회수를 DB 조회수에 반영한다")
    @Test
    void flush() {
        // given
        Long productId = saveProduct();
        productViewCounter.record(productId, "visitor-a");
        productViewCounter.record(productId, "visitor-b");

        // when
        productViewCountFlusher.flush();

        // then
        assertThat(findViewCount(productId)).isEqualTo(2L);
        assertThat(productViewRedisRepository.findPendingCount(productId)).isZero();
        assertThat(productViewRedisRepository.findPendingProductIds()).isEmpty();
    }

    @DisplayName("반영할 조회수가 없으면 DB 조회수를 바꾸지 않는다")
    @Test
    void flush_nothingPending() {
        // given
        Long productId = saveProduct();

        // when
        productViewCountFlusher.flush();

        // then
        assertThat(findViewCount(productId)).isZero();
    }

    @DisplayName("이미 반영한 조회수는 다시 반영되지 않는다")
    @Test
    void flush_twice() {
        // given
        Long productId = saveProduct();
        productViewCounter.record(productId, "visitor-a");
        productViewCountFlusher.flush();

        // when
        productViewCountFlusher.flush();

        // then
        assertThat(findViewCount(productId)).isEqualTo(1L);
    }

    @DisplayName("여러 상품의 미반영 조회수를 각각 자기 상품에 반영한다")
    @Test
    void flush_multipleProducts() {
        // given
        Long first = saveProduct();
        Long second = saveProduct();
        productViewCounter.record(first, "visitor-a");
        productViewCounter.record(second, "visitor-a");
        productViewCounter.record(second, "visitor-b");

        // when
        productViewCountFlusher.flush();

        // then
        assertThat(findViewCount(first)).isEqualTo(1L);
        assertThat(findViewCount(second)).isEqualTo(2L);
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3)).getId();
    }

    private long findViewCount(Long productId) {
        return productRepository.findById(productId).orElseThrow().getViewCount();
    }
}
