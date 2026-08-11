package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.TagRepository;
import project.kjhjdh.ibid.product.presentation.dto.ImageConfirmRequest;
import project.kjhjdh.ibid.product.presentation.dto.ProductDetailResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterRequest;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductServiceIntegrationTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final String VISITOR_ID = "visitor-a";

    @Autowired
    private ProductService productService;

    @Autowired
    private TagRepository tagRepository;

    @DisplayName("여러 상품이 같은 이름의 태그를 쓰면 태그는 재사용되어 한 번만 저장된다")
    @Test
    void register_reusesSameTag() {
        // given
        ProductRegisterRequest first = registerRequest("나이키 후드", List.of("나이키", "후드"));
        ProductRegisterRequest second = registerRequest("나이키 데님", List.of(" 나이키 ", "데님"));

        // when
        productService.register(SELLER_ID, first);
        productService.register(SELLER_ID, second);

        // then
        assertThat(tagRepository.count()).isEqualTo(3);
        assertThat(tagRepository.findByName("나이키")).isPresent();
        assertThat(tagRepository.findByName("후드")).isPresent();
        assertThat(tagRepository.findByName("데님")).isPresent();
    }

    @DisplayName("상품 상세 조회는 조회수·이미지·상품상태·태그·배송비를 한 응답에 모두 담는다")
    @Test
    void getProduct_returnsEveryDetailField() {
        // given
        Long productId = productService.register(SELLER_ID, registerRequest("나이키 후드", List.of("나이키", "후드")));
        productService.confirmImages(SELLER_ID, productId,
                new ImageConfirmRequest(List.of("https://image/a.jpg", "https://image/b.jpg")));
        productService.openForSale(SELLER_ID, productId);

        // when
        ProductDetailResponse response = productService.getProduct(productId, VISITOR_ID);

        // then
        assertThat(response.viewCount()).isEqualTo(1L);
        assertThat(response.imageUrls()).containsExactly("https://image/a.jpg", "https://image/b.jpg");
        assertThat(response.tags()).containsExactlyInAnyOrder("나이키", "후드");
        assertThat(response.shippingFee()).isEqualTo(3000);
        assertThat(response.productCondition()).isEqualTo(ProductCondition.LIKE_NEW);
        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(response.createdAt()).isNotNull();
    }

    private ProductRegisterRequest registerRequest(String title, List<String> tags) {
        return new ProductRegisterRequest(title, "상태 좋음", 89000, 3, ProductCondition.LIKE_NEW, tags, 3000);
    }
}
