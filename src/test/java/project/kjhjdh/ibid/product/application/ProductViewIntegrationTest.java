package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductViewIntegrationTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long ABSENT_PRODUCT_ID = 999L;
    private static final String VISITOR_ID = "visitor-a";
    private static final String OTHER_VISITOR_ID = "visitor-b";

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @DisplayName("비로그인 방문자가 상품을 조회하면 조회수가 1 증가한 값이 응답된다")
    @Test
    void getProduct_countsFirstView() {
        // given
        Long productId = saveProduct();

        // when
        long viewCount = productService.getProduct(productId, VISITOR_ID).viewCount();

        // then
        assertThat(viewCount).isEqualTo(1L);
    }

    @DisplayName("같은 방문자가 다시 조회하면 조회수가 늘지 않는다")
    @Test
    void getProduct_doesNotCountDuplicatedView() {
        // given
        Long productId = saveProduct();
        productService.getProduct(productId, VISITOR_ID);

        // when
        long viewCount = productService.getProduct(productId, VISITOR_ID).viewCount();

        // then
        assertThat(viewCount).isEqualTo(1L);
    }

    @DisplayName("다른 방문자가 조회하면 조회수가 늘어난다")
    @Test
    void getProduct_countsOtherVisitor() {
        // given
        Long productId = saveProduct();
        productService.getProduct(productId, VISITOR_ID);

        // when
        long viewCount = productService.getProduct(productId, OTHER_VISITOR_ID).viewCount();

        // then
        assertThat(viewCount).isEqualTo(2L);
    }

    @DisplayName("존재하지 않는 상품을 조회하면 조회 기록이 전혀 남지 않는다")
    @Test
    void getProduct_notFoundLeavesNoTrace() {
        // when & then
        assertThatThrownBy(() -> productService.getProduct(ABSENT_PRODUCT_ID, VISITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        assertThat(stringRedisTemplate.keys("product-view*")).isEmpty();
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3)).getId();
    }
}
