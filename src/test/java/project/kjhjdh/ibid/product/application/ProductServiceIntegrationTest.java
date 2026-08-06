package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.TagRepository;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterRequest;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductServiceIntegrationTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;

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

    private ProductRegisterRequest registerRequest(String title, List<String> tags) {
        return new ProductRegisterRequest(title, "상태 좋음", 89000, 3, ProductCondition.LIKE_NEW, tags, 3000);
    }
}
