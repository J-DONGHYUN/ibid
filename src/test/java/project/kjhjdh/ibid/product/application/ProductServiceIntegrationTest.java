package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductServiceIntegrationTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final String VISITOR_ID = "visitor-a";

    @Autowired
    private ProductService productService;

    @DisplayName("상품 상세 조회는 조회수·이미지·상품상태를 한 응답에 모두 담는다")
    @Test
    void getProduct_returnsEveryDetailField() {
        // given
        Long productId = productService.register(SELLER_ID, registerCommand("나이키 후드"));
        productService.confirmImages(SELLER_ID, productId,
                List.of("https://image/a.jpg", "https://image/b.jpg"));

        // when
        ProductDetailResult result = productService.getProduct(productId, VISITOR_ID);

        // then
        assertThat(result.viewCount()).isEqualTo(1L);
        assertThat(result.imageUrls()).containsExactly("https://image/a.jpg", "https://image/b.jpg");
        assertThat(result.productCondition()).isEqualTo(ProductCondition.LIKE_NEW);
        assertThat(result.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(result.createdAt()).isNotNull();
    }

    private ProductRegisterCommand registerCommand(String title) {
        return new ProductRegisterCommand(title, "상태 좋음", 89000, ProductCondition.LIKE_NEW);
    }
}
